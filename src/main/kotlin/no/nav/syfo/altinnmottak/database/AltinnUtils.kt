package no.nav.syfo.altinnmottak.database

import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import java.sql.ResultSet
import java.util.*

fun ResultSet.toAltinnLpsOppfolgingsplan() = AltinnLpsOppfolgingsplan(
    uuid = UUID.fromString(getString("uuid")),
    lpsFnr = getString("lps_fnr"),
    fnr = getString("fnr"),
    orgnummer = getString("orgnummer"),
    pdf = getBytes("pdf"),
    xml = getString("xml"),
    shouldSendToNav = getBoolean("should_send_to_nav"),
    shouldSendToFastlege = getBoolean("should_send_to_fastlege"),
    sentToNav = getBoolean("sent_to_nav"),
    sentToFastlege = getBoolean("sent_to_fastlege"),
    sendToFastlegeRetryCount = getInt("send_to_fastlege_retry_count"),
    journalpostId = getString("journalpost_id"),
    archiveReference = getString("archive_reference"),
    created = getTimestamp("created").toLocalDateTime(),
    lastChanged = getTimestamp("last_changed").toLocalDateTime(),
)
