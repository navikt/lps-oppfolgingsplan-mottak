package no.nav.syfo.oppfolgingsplanmottak.domain

import no.nav.syfo.client.oppdfgen.domain.LpsPlanPdfData
import no.nav.syfo.client.oppdfgen.domain.OppfolgingsplanOpPdfGenRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val contactPersonEmail: String,
    val employeeHasContributedToPlan: Boolean,
    val employeeHasNotContributedToPlanDescription: String?,
    val lpsName: String,
    val lpsEmail: String?,
) {
    fun toOppfolgingsplanOpPdfGenRequest(
        employeeName: String?,
        employeePhoneNumber: String?,
        employeeEmail: String?,
        employeeAddress: String?,
    ): OppfolgingsplanOpPdfGenRequest {
        val sendPlanTo = getSendToString(this.sendPlanToNav, this.sendPlanToGeneralPractitioner)
        val evaluationDateFormatted = getEvaluationDateFormatted(this.evaluationDate)

        return OppfolgingsplanOpPdfGenRequest(
            LpsPlanPdfData(
                employeeFnr = this.employeeIdentificationNumber,
                employeeName = employeeName,
                employeePhoneNumber = employeePhoneNumber,
                employeeAddress = employeeAddress,
                employeeEmail = employeeEmail,
                typicalWorkday = this.typicalWorkday,
                tasksThatCanStillBeDone = this.tasksThatCanStillBeDone,
                tasksThatCanNotBeDone = this.tasksThatCanNotBeDone,
                previousFacilitation = this.previousFacilitation,
                plannedFacilitation = this.plannedFacilitation,
                otherFacilitationOptions = this.otherFacilitationOptions,
                followUp = this.followUp,
                evaluationDate = evaluationDateFormatted,
                sendPlanToString = sendPlanTo,
                needsHelpFromNav = this.needsHelpFromNav,
                needsHelpFromNavDescription = this.needsHelpFromNavDescription,
                messageToGeneralPractitioner = this.messageToGeneralPractitioner,
                additionalInformation = this.additionalInformation,
                employerContactPersonFullName = this.contactPersonFullName,
                employerContactPersonPhoneNumber = this.contactPersonPhoneNumber,
                employerContactPersonEmail = this.contactPersonEmail,
                employeeHasContributedToPlan = if (this.employeeHasContributedToPlan) "Ja" else "Nei",
                employeeHasNotContributedToPlanDescription = this.employeeHasNotContributedToPlanDescription,
            ),
        )
    }

    private fun getEvaluationDateFormatted(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd. MMMM yyyy"))

    private fun getSendToString(
        nav: Boolean,
        lege: Boolean,
    ): String {
        var sendToStr = ""
        if (nav) {
            sendToStr += "NAV"
        }
        if (nav && lege) {
            sendToStr += ", "
        }
        if (lege) {
            sendToStr += "LEGE"
        }
        return sendToStr
    }
}
