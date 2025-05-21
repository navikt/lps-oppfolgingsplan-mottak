package no.nav.syfo.oppfolgingsplanmottak.validation

import no.nav.syfo.application.exception.EmployeeNotFoundException
import no.nav.syfo.application.exception.FollowUpPlanDTOValidationException
import no.nav.syfo.application.exception.NoActiveEmploymentException
import no.nav.syfo.application.exception.NoActiveSentSykmeldingException
import no.nav.syfo.application.exception.PdlBadRequestException
import no.nav.syfo.application.exception.PdlException
import no.nav.syfo.application.exception.PdlNotFoundException
import no.nav.syfo.application.exception.PdlServerException
import no.nav.syfo.application.exception.PdlServiceException
import no.nav.syfo.application.exception.PdlUnauthorizedException
import no.nav.syfo.client.aareg.ArbeidsforholdOversiktClient
import no.nav.syfo.client.oppdfgen.PdlUtils
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import org.slf4j.LoggerFactory

val TEST_FNR_LIST = listOf("05908399546", "01898299631")

class FollowUpPlanValidator(
    private val pdlUtils: PdlUtils,
    private val sykmeldingService: SendtSykmeldingService,
    private val arbeidsforholdOversiktClient: ArbeidsforholdOversiktClient,
    private val isDev: Boolean,
) {
    private val log = LoggerFactory.getLogger(FollowUpPlanValidator::class.qualifiedName)

    suspend fun validateFollowUpPlanDTO(followUpPlanDTO: FollowUpPlanDTO, employerOrgnr: String) {
        validatePlan(followUpPlanDTO)

        validateEmployeeInformation(followUpPlanDTO, employerOrgnr, isDev)
    }

    private fun validatePlan(followUpPlanDTO: FollowUpPlanDTO) {
        if (followUpPlanDTO.needsHelpFromNav == true && !followUpPlanDTO.sendPlanToNav) {
            throw FollowUpPlanDTOValidationException("needsHelpFromNav cannot be true if sendPlanToNav is false")
        }
        if (followUpPlanDTO.needsHelpFromNav == true && followUpPlanDTO.needsHelpFromNavDescription.isNullOrBlank()) {
            throw FollowUpPlanDTOValidationException(
                "needsHelpFromNavDescription is obligatory if needsHelpFromNav is true"
            )
        }
        if (!followUpPlanDTO.employeeHasContributedToPlan &&
            followUpPlanDTO.employeeHasNotContributedToPlanDescription.isNullOrBlank()
        ) {
            throw FollowUpPlanDTOValidationException(
                "employeeHasNotContributedToPlanDescription is mandatory if employeeHasContributedToPlan = false"
            )
        }
        if (followUpPlanDTO.employeeHasContributedToPlan &&
            followUpPlanDTO.employeeHasNotContributedToPlanDescription?.isNotEmpty() == true
        ) {
            throw FollowUpPlanDTOValidationException(
                "employeeHasNotContributedToPlanDescription should not be set if employeeHasContributedToPlan = true"
            )
        }
    }

    private suspend fun validateEmployeeInformation(
        followUpPlanDTO: FollowUpPlanDTO,
        employerOrgnr: String,
        isDev: Boolean
    ) {
        if (!followUpPlanDTO.employeeIdentificationNumber.matches(Regex("\\d{11}"))) {
            throw FollowUpPlanDTOValidationException("Invalid employee identification number")
        }

        if (isDev && TEST_FNR_LIST.contains(followUpPlanDTO.employeeIdentificationNumber)) {
            return
        }

        val validOrgnumbers = validateArbeidsforhold(followUpPlanDTO, employerOrgnr)

        validateSykmelding(followUpPlanDTO, validOrgnumbers)

        validatePersonExists(followUpPlanDTO.employeeIdentificationNumber)
    }

    private suspend fun validatePersonExists(fnr: String) {
        try {
            val personInfo = pdlUtils.getPersonInfoWithRetry(fnr)

            if (personInfo == null) {
                log.warn("Person with fnr not found in PDL after retry attempts")
                throw EmployeeNotFoundException("Could not find requested person in our systems")
            }
        } catch (e: PdlNotFoundException) {
            // This is the correct case for EmployeeNotFoundException
            log.warn("Person with fnr not found in PDL")
            throw EmployeeNotFoundException("Could not find requested person in our systems")
        } catch (e: PdlBadRequestException) {
            // Bad input data - likely an invalid FNR format that passed regex check
            log.error("Bad request when checking person in PDL: ${e.message}")
            throw FollowUpPlanDTOValidationException("Invalid employee identification number format")
        } catch (e: PdlUnauthorizedException) {
            // Authorization issues
            log.error("Unauthorized access to PDL: ${e.message}")
            throw PdlServiceException("Authentication issue when validating employee. Try again later.")
        } catch (e: PdlServerException) {
            // Server-side PDL issues - temporary error
            log.error("PDL server error: ${e.message}")
            throw PdlServiceException("Temporary issue with person lookup service. Try again later.")
        } catch (e: PdlException) {
            // Other PDL issues
            log.error("PDL error when validating employee: ${e.message}")
            throw PdlServiceException("Error occurred during person validation. Please try again later.")
        } catch (e: Exception) {
            // Unexpected errors
            log.error("Unexpected error during person validation: ${e.message}", e)
            throw PdlServiceException("Unexpected error during validation. Please try again later.")
        }
    }

    private fun validateSykmelding(
        followUpPlanDTO: FollowUpPlanDTO,
        validOrgnumbers: List<String?>
    ) {
        val activeSykmeldinger: List<Sykmeldingsperiode> = sykmeldingService.getActiveSendtSykmeldingsperioder(
            followUpPlanDTO.employeeIdentificationNumber,
        )

        val hasActiveSendtSykmelding = activeSykmeldinger.any { validOrgnumbers.contains(it.organizationNumber) }
        if (!hasActiveSendtSykmelding) {
            throw NoActiveSentSykmeldingException("No active sykmelding sent to employer")
        }
    }

    private suspend fun validateArbeidsforhold(
        followUpPlanDTO: FollowUpPlanDTO,
        employerOrgnr: String
    ): List<String?> {
        val arbeidsforholdOversikt =
            arbeidsforholdOversiktClient.getArbeidsforhold(followUpPlanDTO.employeeIdentificationNumber)

        val activeArbeidsforhold = arbeidsforholdOversikt?.arbeidsforholdoversikter?.firstOrNull {
            it.opplysningspliktig.getJuridiskOrgnummer() == employerOrgnr ||
                it.arbeidssted.getOrgnummer() == employerOrgnr
        }

        if (activeArbeidsforhold == null) {
            throw NoActiveEmploymentException("No active employment relationship found for given orgnumber")
        }

        val validOrgnumbers = listOf(
            activeArbeidsforhold.opplysningspliktig.getJuridiskOrgnummer(),
            activeArbeidsforhold.arbeidssted.getOrgnummer(),
        )
        return validOrgnumbers
    }
}
