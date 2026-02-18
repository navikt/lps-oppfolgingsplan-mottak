@file:Suppress("TooManyFunctions", "MagicNumber")

package no.nav.syfo.altinnmottak.database

import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.application.database.toObject
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

fun DatabaseInterface.storeFnr(
    uuid: UUID,
    fnr: String,
): Int {
    val updateStatement =
        """
        UPDATE ALTINN_LPS
        SET fnr = ?, last_changed = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setString(1, fnr)
                it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                it.setObject(3, uuid)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
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

fun DatabaseInterface.updateJournalpostId(
    uuid: UUID,
    journalpostId: String,
): Int {
    val updateStatement =
        """
        UPDATE ALTINN_LPS
        SET journalpost_id = ?, last_changed = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
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
    val updateStatement =
        """
        UPDATE ALTINN_LPS
        SET sent_to_nav = TRUE, last_changed = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                it.setObject(2, uuid)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
    }
}

fun DatabaseInterface.setSentToFastlegeTrue(uuid: UUID): Int {
    val updateStatement =
        """
        UPDATE ALTINN_LPS
        SET sent_to_fastlege = TRUE, last_changed = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                it.setObject(2, uuid)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
    }
}

fun DatabaseInterface.updateSendToFastlegeRetryCount(
    uuid: UUID,
    prevCount: Int,
): Int {
    val updateStatement =
        """
        UPDATE ALTINN_LPS
        SET send_to_fastlege_retry_count = ?, last_changed = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setInt(1, prevCount + 1)
                it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                it.setObject(3, uuid)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
    }
}

fun DatabaseInterface.getAltinnLpsOppfolgingsplanByUuid(lpsUUID: UUID): AltinnLpsOppfolgingsplan {
    val queryStatement =
        """
        SELECT *
        FROM ALTINN_LPS
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setObject(1, lpsUUID)
            it.executeQuery().toObject { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getAltinnLpsOppfolgingsplanWithoutMostRecentFnr(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement =
        """
        SELECT *
        FROM ALTINN_LPS
        WHERE fnr is null
        AND skip_retry is not true 
        LIMIT 50
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getAltinnLpsOppfolgingsplanWithoutGeneratedPdf(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement =
        """
        SELECT *
        FROM ALTINN_LPS
        WHERE fnr is not null
        AND pdf is null
        AND skip_retry is not true
        LIMIT 50
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getAltinnLpsOppfolgingsplanNotYetSentToNav(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement =
        """
        SELECT *
        FROM ALTINN_LPS
        WHERE pdf is not null
        AND should_send_to_nav
        AND NOT sent_to_nav
        AND migrated = false
        AND skip_retry is not true
        LIMIT 50
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getAltinnLpsOppfolgingsplanNotYetSentToFastlege(retryThreshold: Int): List<AltinnLpsOppfolgingsplan> {
    val queryStatement =
        """
        SELECT *
        FROM ALTINN_LPS
        WHERE pdf is not null
        AND should_send_to_fastlege
        AND NOT sent_to_fastlege 
        AND send_to_fastlege_retry_count <= ?
        AND migrated = false
        AND skip_retry is not true
        LIMIT 50
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setInt(1, retryThreshold)
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}

fun DatabaseInterface.getAltinnLpsOppfolgingsplanNotYetSentToDokarkiv(): List<AltinnLpsOppfolgingsplan> {
    val queryStatement =
        """
        SELECT *
        FROM ALTINN_LPS
        WHERE sent_to_nav
        AND pdf is not null
        AND journalpost_id is null
        AND migrated = false
        AND skip_retry is not true
        LIMIT 50
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toList { toAltinnLpsOppfolgingsplan() }
        }
    }
}
