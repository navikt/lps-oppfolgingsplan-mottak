package no.nav.syfo.db

import no.nav.syfo.db.domain.AltinnLpsOppfolgingsplan
import java.sql.ResultSet
import java.util.*

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
    shouldSendToGp = getBoolean("should_send_to_gp"),
    sentToGp = getBoolean("sent_to_gp"),
    sendToGpRetryCount = getLong("send_to_gp_retry_count"),
    originallyCreated = getTimestamp("originally_created").toLocalDateTime(),
    created = getTimestamp("created").toLocalDateTime(),
    lastChanged = getTimestamp("last_changed").toLocalDateTime(),
)
