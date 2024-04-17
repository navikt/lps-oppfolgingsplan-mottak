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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FollowUpPlan

        if (uuid != other.uuid) return false
        if (isSentToGeneralPractitionerStatus != other.isSentToGeneralPractitionerStatus) return false
        if (isSentToNavStatus != other.isSentToNavStatus) return false
        if (pdf != null) {
            if (other.pdf == null) return false
            if (!pdf.contentEquals(other.pdf)) return false
        } else if (other.pdf != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + (isSentToGeneralPractitionerStatus?.hashCode() ?: 0)
        result = 31 * result + (isSentToNavStatus?.hashCode() ?: 0)
        result = 31 * result + (pdf?.contentHashCode() ?: 0)
        return result
    }
}
