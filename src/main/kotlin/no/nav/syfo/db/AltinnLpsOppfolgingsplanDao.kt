package no.nav.syfo.db

import no.nav.syfo.db.domain.AltinnLpsOppfolgingsplan
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@Suppress("MagicNumber")
fun DatabaseInterface.storeAltinnLps(altinnOP: AltinnLpsOppfolgingsplan) {
    val insertStatement = """
        INSERT INTO ALTINN_LPS (
            archive_reference,
            uuid,
            lps_fnr,
            orgnummer,
            xml,
            should_send_to_nav,
            should_send_to_gp,
            sent_to_gp,
            originally_created,
            created,
            last_changed
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setString(1, altinnOP.archiveReference)
            it.setObject(2, altinnOP.uuid)
            it.setString(3, altinnOP.lpsFnr)
            it.setString(4, altinnOP.orgnummer)
            it.setString(5, altinnOP.xml)
            it.setBoolean(6, altinnOP.shouldSendToNav)
            it.setBoolean(7, altinnOP.shouldSendToGp)
            it.setBoolean(8, altinnOP.sentToGp)
            it.setTimestamp(9, Timestamp.valueOf(altinnOP.originallyCreated))
            it.setTimestamp(10, Timestamp.valueOf(altinnOP.created))
            it.setTimestamp(11, Timestamp.valueOf(altinnOP.lastChanged))
            it.executeUpdate()
        }
        connection.commit()
    }
}

@Suppress("MagicNumber")
fun DatabaseInterface.storeFnr(uuid: UUID, fnr: String): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET fnr = ?, last_changed = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setString(1, fnr)
            it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
            it.setObject(3, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

@Suppress("MagicNumber")
fun DatabaseInterface.storePdf(uuid: UUID, pdfBytes: ByteArray): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET pdf = ?, last_changed = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setBytes(1, pdfBytes)
            it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
            it.setObject(3, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

@Suppress("MagicNumber")
fun DatabaseInterface.setSendToGpTrue(uuid: UUID): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET sent_to_gp = TRUE
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setObject(1, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

@Suppress("MagicNumber")
fun DatabaseInterface.getLpsByUuid(lpsUUID: UUID): AltinnLpsOppfolgingsplan {
    val queryStatement = """
        SELECT *
        FROM ALTINN_LPS
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setObject(1, lpsUUID)
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }.first()
    }
}

fun DatabaseInterface.getLpsWithoutMostRecentFnr(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement = """
        SELECT *
        FROM ALTINN_LPS
        WHERE fnr is null
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getLpsWithoutGeneratedPdf(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement = """
        SELECT *
        FROM ALTINN_LPS
        WHERE fnr is not null
        AND pdf is null
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}
