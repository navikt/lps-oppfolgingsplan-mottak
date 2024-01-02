@file:Suppress("TooManyFunctions", "MagicNumber")
package no.nav.syfo.db

import no.nav.syfo.db.domain.AltinnLpsOppfolgingsplan
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

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
            originally_created,
            created,
            last_changed
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            it.setTimestamp(8, Timestamp.valueOf(altinnOP.originallyCreated))
            it.setTimestamp(9, Timestamp.valueOf(altinnOP.created))
            it.setTimestamp(10, Timestamp.valueOf(altinnOP.lastChanged))
            it.executeUpdate()
        }
        connection.commit()
    }
}

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

fun DatabaseInterface.updateJournalpostId(uuid: UUID, journalpostId: String): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET journalpost_id = ?, last_changed = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setString(1, journalpostId)
            it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
            it.setObject(3, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

fun DatabaseInterface.setSentToNavTrue(uuid: UUID): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET sent_to_nav = TRUE, last_changed = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
            it.setObject(2, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

fun DatabaseInterface.setSentToGpTrue(uuid: UUID): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET sent_to_gp = TRUE, last_changed = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
            it.setObject(2, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

fun DatabaseInterface.updateSendToGpRetryCount(uuid: UUID, prevCount: Int): Int {
    val updateStatement = """
        UPDATE ALTINN_LPS
        SET send_to_gp_retry_count = ?, last_changed = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setInt(1, prevCount + 1)
            it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
            it.setObject(3, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

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


fun DatabaseInterface.getLpsNotYetSentToNav(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement = """
        SELECT *
        FROM ALTINN_LPS
        WHERE pdf is not null
        AND should_send_to_nav
        AND NOT sent_to_nav
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getLpsNotYetSentToGp(retryThreshold: Int): List<AltinnLpsOppfolgingsplan> {
    val queryStatement = """
        SELECT *
        FROM ALTINN_LPS
        WHERE pdf is not null
        AND should_send_to_gp
        AND NOT sent_to_gp 
        AND send_to_gp_retry_count <= ?
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setInt(1, retryThreshold)
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getLpsNotYetSentToDokarkiv(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement = """
        SELECT *
        FROM ALTINN_LPS
        WHERE sent_to_nav
        AND pdf is not null
        AND journalpost_id is null
    """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}
