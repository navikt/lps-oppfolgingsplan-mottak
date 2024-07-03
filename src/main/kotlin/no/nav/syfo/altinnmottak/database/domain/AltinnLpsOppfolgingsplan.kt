package no.nav.syfo.altinnmottak.database.domain

import java.time.LocalDateTime
import java.util.*

data class AltinnLpsOppfolgingsplan(
    val uuid: UUID,
    val lpsFnr: String,
    val fnr: String?,
    val orgnummer: String,
    val pdf: ByteArray?,
    val xml: String,
    val shouldSendToNav: Boolean,
    val shouldSendToFastlege: Boolean,
    val sentToNav: Boolean,
    val sentToFastlege: Boolean,
    val sendToFastlegeRetryCount: Int,
    val journalpostId: String?,
    val archiveReference: String?,
    val created: LocalDateTime,
    val lastChanged: LocalDateTime,
) {
    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AltinnLpsOppfolgingsplan

        if (uuid != other.uuid) return false
        if (lpsFnr != other.lpsFnr) return false
        if (fnr != other.fnr) return false
        if (orgnummer != other.orgnummer) return false
        if (pdf != null) {
            if (other.pdf == null) return false
            if (!pdf.contentEquals(other.pdf)) return false
        } else if (other.pdf != null) return false
        if (xml != other.xml) return false
        if (shouldSendToNav != other.shouldSendToNav) return false
        if (shouldSendToFastlege != other.shouldSendToFastlege) return false
        if (sentToNav != other.sentToNav) return false
        if (sentToFastlege != other.sentToFastlege) return false
        if (sendToFastlegeRetryCount != other.sendToFastlegeRetryCount) return false
        if (journalpostId != other.journalpostId) return false
        if (archiveReference != other.archiveReference) return false
        if (created != other.created) return false
        if (lastChanged != other.lastChanged) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + lpsFnr.hashCode()
        result = 31 * result + (fnr?.hashCode() ?: 0)
        result = 31 * result + orgnummer.hashCode()
        result = 31 * result + (pdf?.contentHashCode() ?: 0)
        result = 31 * result + xml.hashCode()
        result = 31 * result + shouldSendToNav.hashCode()
        result = 31 * result + shouldSendToFastlege.hashCode()
        result = 31 * result + sentToNav.hashCode()
        result = 31 * result + sentToFastlege.hashCode()
        result = 31 * result + sendToFastlegeRetryCount
        result = 31 * result + (journalpostId?.hashCode() ?: 0)
        result = 31 * result + (archiveReference?.hashCode() ?: 0)
        result = 31 * result + created.hashCode()
        result = 31 * result + lastChanged.hashCode()
        return result
    }
}
