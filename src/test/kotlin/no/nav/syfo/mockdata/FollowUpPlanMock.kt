package no.nav.syfo.mockdata

import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import java.time.LocalDate

fun createDefaultFollowUpPlan(employeeIdentificationNumber: String): FollowUpPlanDTO {
    val followUpPlanDTO = FollowUpPlanDTO(
        employeeIdentificationNumber = employeeIdentificationNumber,
        typicalWorkday = "Typical workday description",
        tasksThatCanStillBeDone = "Tasks that can still be done",
        tasksThatCanNotBeDone = "Tasks that cannot be done",
        previousFacilitation = "Previous facilitation description",
        plannedFacilitation = "Planned facilitation description",
        otherFacilitationOptions = "Other facilitation options",
        followUp = "Follow up description",
        evaluationDate = LocalDate.now(),
        sendPlanToNav = true,
        needsHelpFromNav = false,
        needsHelpFromNavDescription = null,
        sendPlanToGeneralPractitioner = true,
        messageToGeneralPractitioner = "Message to general practitioner",
        additionalInformation = "Additional information",
        contactPersonFullName = "Contact person full name",
        contactPersonPhoneNumber = "12345678",
        contactPersonEmail = "some@email.com",
        employeeHasContributedToPlan = true,
        employeeHasNotContributedToPlanDescription = null,
        lpsName = "LPS name"
    )
    return followUpPlanDTO
}

data class FollowUpPlanMockDTO(
    val employeeIdentificationNumber: String?,
    val typicalWorkday: String?,
    val tasksThatCanStillBeDone: String?,
    val tasksThatCanNotBeDone: String?,
    val previousFacilitation: String?,
    val plannedFacilitation: String?,
    val otherFacilitationOptions: String?,
    val followUp: String?,
    val evaluationDate: LocalDate?,
    val sendPlanToNav: Boolean?,
    val needsHelpFromNav: Boolean?,
    val needsHelpFromNavDescription: String?,
    val sendPlanToGeneralPractitioner: Boolean?,
    val messageToGeneralPractitioner: String?,
    val additionalInformation: String?,
    val contactPersonFullName: String?,
    val contactPersonPhoneNumber: String?,
    val contactPersonEmail: String?,
    val employeeHasContributedToPlan: Boolean?,
    val employeeHasNotContributedToPlanDescription: String?,
    val lpsName: String?
)

fun createDefaultFollowUpPlanMockDTO(employeeIdentificationNumber: String): FollowUpPlanMockDTO {
    val followUpPlanDTO = FollowUpPlanMockDTO(
        employeeIdentificationNumber = employeeIdentificationNumber,
        typicalWorkday = "Typical workday description",
        tasksThatCanStillBeDone = "Tasks that can still be done",
        tasksThatCanNotBeDone = "Tasks that cannot be done",
        previousFacilitation = "Previous facilitation description",
        plannedFacilitation = "Planned facilitation description",
        otherFacilitationOptions = "Other facilitation options",
        followUp = "Follow up description",
        evaluationDate = LocalDate.now(),
        sendPlanToNav = true,
        needsHelpFromNav = false,
        needsHelpFromNavDescription = null,
        sendPlanToGeneralPractitioner = true,
        messageToGeneralPractitioner = "Message to general practitioner",
        additionalInformation = "Additional information",
        contactPersonFullName = "Contact person full name",
        contactPersonPhoneNumber = "12345678",
        contactPersonEmail = "some@email.com",
        employeeHasContributedToPlan = true,
        employeeHasNotContributedToPlanDescription = null,
        lpsName = "LPS name"
    )
    return followUpPlanDTO
}

val randomFollowUpPlanDTO = createDefaultFollowUpPlan("12345678901")
val randomFollowUpPlanMockDTO = createDefaultFollowUpPlanMockDTO("12345678901")
