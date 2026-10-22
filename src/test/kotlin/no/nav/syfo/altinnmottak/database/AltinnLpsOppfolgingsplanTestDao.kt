@file:Suppress("MagicNumber")

package no.nav.syfo.altinnmottak.database

import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.application.database.DatabaseInterface
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

fun DatabaseInterface.storeAltinnLpsOppfolgingsplan(altinnLpsPlan: AltinnLpsOppfolgingsplan) {
    val insertStatement =
        """
        INSERT INTO ALTINN_LPS (
            uuid,
            lps_fnr,
            orgnummer,
            xml,
            should_send_to_nav,
            should_send_to_fastlege,
            archive_reference,
            created,
            last_changed
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setObject(1, altinnLpsPlan.uuid)
            it.setString(2, altinnLpsPlan.lpsFnr)
            it.setString(3, altinnLpsPlan.orgnummer)
            it.setString(4, altinnLpsPlan.xml)
            it.setBoolean(5, altinnLpsPlan.shouldSendToNav)
            it.setBoolean(6, altinnLpsPlan.shouldSendToFastlege)
            it.setString(7, altinnLpsPlan.archiveReference)
            it.setTimestamp(8, Timestamp.valueOf(altinnLpsPlan.created))
            it.setTimestamp(9, Timestamp.valueOf(altinnLpsPlan.lastChanged))
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.storePdf(
    uuid: UUID,
    pdfBytes: ByteArray,
): Int {
    val updateStatement =
        """
        UPDATE ALTINN_LPS
        SET pdf = ?, last_changed = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setBytes(1, pdfBytes)
                it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                it.setObject(3, uuid)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
    }
}
