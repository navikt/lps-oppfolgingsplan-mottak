package no.nav.syfo.db.domain

import java.time.LocalDateTime
import java.util.*

data class AltinnLpsOppfolgingsplan(
    val archiveReference: String,
    val uuid: UUID,
    val lpsFnr: String,
    val fnr: String?,
    val orgnummer: String,
    val pdf: ByteArray?,
    val xml: String,
    val shouldSendToNav: Boolean,
    val shouldSendToGp: Boolean,
    val sentToGp: Boolean,
    val sendToGpRetryCount: Int,
    val originallyCreated: LocalDateTime,
    val created: LocalDateTime,
    val lastChanged: LocalDateTime,
)
