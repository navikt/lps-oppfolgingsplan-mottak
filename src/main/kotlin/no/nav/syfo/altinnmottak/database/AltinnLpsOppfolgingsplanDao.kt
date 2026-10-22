@file:Suppress("TooManyFunctions", "MagicNumber")

package no.nav.syfo.altinnmottak.database

import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

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
