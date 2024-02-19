package no.nav.syfo.oppfolgingsplanmottak.domain

import java.time.LocalDate

data class FollowUpPlanDTO(
    val employeeIdentificationNumber: String,
    val typicalWorkday: String,
    val tasksThatCanStillBeDone: String,
    val tasksThatCanNotBeDone: String,
    val previousFacilitation: String,
    val plannedFacilitation: String,
    val otherFacilitationOptions: String?,
    val followUp: String,
    val evaluationDate: LocalDate,
    val sendPlanToNav: Boolean,
    val needsHelpFromNav: Boolean?,
    val needsHelpFromNavDescription: String?,
    val sendPlanToGeneralPractitioner: Boolean,
    val messageToGeneralPractitioner: String?,
    val additionalInformation: String?,
    val contactPersonFullName: String,
    val contactPersonPhoneNumber: String,
    val employeeHasContributedToPlan: Boolean,
    val employeeHasNotContributedToPlanDescription: String?,
    val lpsName: String
) {
    init {
        fun contributionDescriptionUsedCorrectly(): Boolean {
            if (employeeHasContributedToPlan) {
                return employeeHasNotContributedToPlanDescription == null
            }
            return employeeHasNotContributedToPlanDescription != null
        }

        require(!(needsHelpFromNav == true && !sendPlanToNav)) {
            "needsHelpFromNav cannot be true if sendPlanToNav is false"
        }
        require(needsHelpFromNav != true || !needsHelpFromNavDescription.isNullOrBlank()) {
            "needsHelpFromNavDescription is obligatory if needsHelpFromNav is true"
        }
        require(contributionDescriptionUsedCorrectly()) {
            "employeeHasNotContributedToPlanDescription is mandatory and can only be used if employeeHasContributedToPlan = false"
        }
    }
}
