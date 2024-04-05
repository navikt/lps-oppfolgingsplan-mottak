package no.nav.syfo.client.oppdfgen.domain

data class OppfolgingsplanOpPdfGenRequest(
    val lpsPlanData: LpsPlanPdfData,
)

class LpsPlanPdfData(
    val employeeFnr: String,
    val employeeName: String?,
    val employeePhoneNumber: String?,
    val employeeAddress: String?,
    val employeeEmail: String?,
    val typicalWorkday: String,
    val tasksThatCanStillBeDone: String,
    val tasksThatCanNotBeDone: String,
    val previousFacilitation: String,
    val plannedFacilitation: String,
    val otherFacilitationOptions: String?,
    val followUp: String,
    val evaluationDate: String,
    val sendPlanToString: String,
    val needsHelpFromNav: Boolean?,
    val needsHelpFromNavDescription: String?,
    val messageToGeneralPractitioner: String?,
    val additionalInformation: String?,
    val employerContactPersonFullName: String,
    val employerContactPersonPhoneNumber: String,
    val employerContactPersonEmail: String,
    val employeeHasContributedToPlan: String,
    val employeeHasNotContributedToPlanDescription: String?,
)
