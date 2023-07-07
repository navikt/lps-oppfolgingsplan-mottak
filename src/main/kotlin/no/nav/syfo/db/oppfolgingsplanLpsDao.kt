package no.nav.syfo.db

import no.nav.syfo.api.lps.OppfolgingsplanDTO
import no.nav.syfo.util.gsonSerializer
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

val gsonSerializer = gsonSerializer()

@Suppress("MagicNumber")
fun DatabaseInterface.storeLps(oppfolgingsplanDTO: OppfolgingsplanDTO, version: Short) {
    val insertStatement = """
        INSERT INTO OPPFOLGINGSPLAN_LPS (
            uuid,
            fnr,
            virksomhetsnummer,
            mottaker,
            utfyllingsdato,
            innhold,
            versjon,
            opprettet,
            sist_endret
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    val now = Timestamp.valueOf(LocalDateTime.now())
    val entryUuid = UUID.randomUUID()
    val metadata = oppfolgingsplanDTO.oppfolgingsplanMeta
    val fnr = metadata.sykmeldtFnr
    val virksomhetsnummer = metadata.virksomhet.virksomhetsnummer
    val mottaker = "${metadata.mottaker}"
    val utfyllingsdato = metadata.utfyllingsdato
    val content = gsonSerializer.toJson(oppfolgingsplanDTO)

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setObject(1, entryUuid)
            it.setString(2, fnr)
            it.setString(3, virksomhetsnummer)
            it.setString(4, mottaker)
            it.setTimestamp(5, Timestamp.valueOf(utfyllingsdato))
            it.setString(6, content)
            it.setShort(7, version)
            it.setTimestamp(8, now)
            it.setTimestamp(9, now)
            it.executeUpdate()
        }
        connection.commit()
    }
}
