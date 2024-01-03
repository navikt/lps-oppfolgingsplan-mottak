package no.nav.syfo.db

import no.nav.syfo.db.domain.AltinnLpsOppfolgingsplan
import java.sql.ResultSet
import java.util.*

fun <T> ResultSet.toObject(mapper: ResultSet.() -> T): T {
    next()
    return mapper()
}

fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
    while (next()) {
        add(mapper())
    }
}

fun ResultSet.toAltinnLpsOppfolgingsplan() = AltinnLpsOppfolgingsplan(
    archiveReference = getString("archive_reference"),
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
    originallyCreated = getTimestamp("originally_created").toLocalDateTime(),
    created = getTimestamp("created").toLocalDateTime(),
    lastChanged = getTimestamp("last_changed").toLocalDateTime(),
)
