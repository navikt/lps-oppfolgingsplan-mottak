package no.nav.syfo.mockdata

import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import java.time.LocalDate

fun createFollowUpPlan(employeeIdentificationNumber: String): FollowUpPlanDTO {
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

val randomFollowUpPlanDTO = createFollowUpPlan("123456789")
