package no.nav.syfo.oppfolgingsplanmottak.validation

import no.nav.syfo.application.exception.EmployeeNotFoundException
import no.nav.syfo.application.exception.FollowUpPlanDTOValidationException
import no.nav.syfo.application.exception.NoActiveSentSykmeldingException
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.sykmelding.service.SendtSykmeldingService

class FollowUpPlanValidator(
    private val pdlClient: PdlClient,
    private val sykmeldingService: SendtSykmeldingService
) {
    suspend fun validateFollowUpPlanDTO(followUpPlanDTO: FollowUpPlanDTO, employerOrgnr: String, isDev: Boolean) {
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

        val hasActiveSendtSykmelding =
            sykmeldingService.hasActiveSentSykmelding(
                employerOrgnr,
                followUpPlanDTO.employeeIdentificationNumber,
                isDev
            )

        validateEmployeeInformation(followUpPlanDTO.employeeIdentificationNumber, hasActiveSendtSykmelding)
    }

    private suspend fun validateEmployeeInformation(
        employeeIdentificationNumber: String,
        hasActiveSendtSykmelding: Boolean,
    ) {
        if (!employeeIdentificationNumber.matches(Regex("\\d{11}"))) {
            throw FollowUpPlanDTOValidationException("Invalid employee identification number")
        }

        if (!hasActiveSendtSykmelding) {
            throw NoActiveSentSykmeldingException("No active sykmelding sent to employer")
        }

        if (pdlClient.getPersonInfo(employeeIdentificationNumber) == null) {
            throw EmployeeNotFoundException("Could not find requested person in our systems")
        }
    }
}
