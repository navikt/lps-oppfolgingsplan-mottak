package no.nav.syfo.veileder.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.veileder.domain.OppfolgingsplanLPS
import java.sql.ResultSet
import java.util.*

fun DatabaseInterface.getOppfolgingsplanerForVeileder(ident: PersonIdent): List<OppfolgingsplanLPS> {
    val queryStatement = """
            SELECT uuid, lps_fnr as fnr, orgnummer as virksomhetsnummer, created as opprettet, last_changed as sistEndret
            FROM ALTINN_LPS
            WHERE lps_fnr = ?
            AND should_send_to_nav = true
            AND pdf IS NOT NULL 
            UNION
            SELECT uuid, employee_identification_number as fnr, organization_number as virksomhetsnummer, created_at as opprettet, last_updated_at as sistEndret
            FROM FOLLOW_UP_PLAN_LPS_V1
            WHERE employee_identification_number = ?
            AND send_plan_to_nav = true
            AND pdf IS NOT NULL 
            ORDER BY sistEndret DESC
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setString(1, ident.value)
            it.setString(2, ident.value)
            it.executeQuery().toList { toOppfolgingsplanLPS() }
        }
    }
}

fun DatabaseInterface.getOppfolgingsplanPdf(
    oppfolgingsplanLPSUUID: UUID,
): Pair<PersonIdent, ByteArray>? {
    val queryStatement = """
        SELECT lps_fnr as fnr, pdf
            FROM ALTINN_LPS
            WHERE uuid = ?
            AND should_send_to_nav = true
            AND pdf IS NOT NULL 
        UNION
        SELECT employee_identification_number as fnr, pdf
            FROM FOLLOW_UP_PLAN_LPS_V1
            WHERE uuid = ?
            AND send_plan_to_nav = true
            AND pdf IS NOT NULL 
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setObject(1, oppfolgingsplanLPSUUID)
            it.setObject(2, oppfolgingsplanLPSUUID)
            it.executeQuery().toList { toPdf() }.firstOrNull()
        }
    }
}

fun ResultSet.toOppfolgingsplanLPS() = OppfolgingsplanLPS(
    uuid = UUID.fromString(getString("uuid")),
    fnr = getString("fnr"),
    virksomhetsnummer = getString("virksomhetsnummer"),
    opprettet = getTimestamp("opprettet").toLocalDateTime(),
    sistEndret = getTimestamp("sistEndret").toLocalDateTime(),
)

fun ResultSet.toPdf() = Pair<PersonIdent, ByteArray>(
    PersonIdent(getString("fnr")),
    getBytes("pdf")
)
