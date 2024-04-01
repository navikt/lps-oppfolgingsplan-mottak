package no.nav.syfo.oppfolgingsplanmottak.domain

data class FollowUpPlan(
    val uuid: String,
    val isSentToGeneralPractitionerStatus: Boolean?,
    val isSentToNavStatus: Boolean?,
    val pdf: ByteArray?,
) {
    fun toFollowUpPlanResponse(): FollowUpPlanResponse {
        return FollowUpPlanResponse(this.uuid, this.isSentToGeneralPractitionerStatus, this.isSentToNavStatus)
    }
}
