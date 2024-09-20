package no.nav.syfo.oppfolgingsplanmottak.validation

import no.nav.syfo.application.exception.EmployeeNotFoundException
import no.nav.syfo.application.exception.FollowUpPlanDTOValidationException
import no.nav.syfo.application.exception.NoActiveEmploymentException
import no.nav.syfo.application.exception.NoActiveSentSykmeldingException
import no.nav.syfo.client.aareg.ArbeidsforholdOversiktClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.service.SendtSykmeldingService

class FollowUpPlanValidator(
    private val pdlClient: PdlClient,
    private val sykmeldingService: SendtSykmeldingService,
    private val arbeidsforholdOversiktClient: ArbeidsforholdOversiktClient,
    private val isDev: Boolean,
) {
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

        if (isDev && followUpPlanDTO.employeeIdentificationNumber == "01898299631") {
            return
        }

        val validOrgnumbers = validateArbeidsforhold(followUpPlanDTO, employerOrgnr)

        validateSykmelding(followUpPlanDTO, validOrgnumbers)

        if (pdlClient.getPersonInfo(followUpPlanDTO.employeeIdentificationNumber) == null) {
            throw EmployeeNotFoundException("Could not find requested person in our systems")
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
