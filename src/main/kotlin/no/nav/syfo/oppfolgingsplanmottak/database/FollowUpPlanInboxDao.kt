package no.nav.syfo.oppfolgingsplanmottak.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toNullableObject
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanInbox
import no.nav.syfo.oppfolgingsplanmottak.domain.InboxStatus
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

fun DatabaseInterface.storeFollowUpPlanInbox(followUpPlanInbox: FollowUpPlanInbox) {
    val insertStatement =
        """
        INSERT INTO FOLLOW_UP_PLAN_INBOX (
            correlation_id,
            organization_number,
            lps_orgnumber,
            raw_payload,
            status,
            status_message,
            received_at,
            validated_at,
            processed_at,
            updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setString(1, followUpPlanInbox.correlationId)
            it.setString(2, followUpPlanInbox.organizationNumber)
            it.setString(3, followUpPlanInbox.lpsOrgnumber)
            it.setString(4, followUpPlanInbox.rawPayload)
            it.setString(5, followUpPlanInbox.status.name)
            it.setString(6, followUpPlanInbox.statusMessage)
            it.setTimestamp(7, Timestamp.valueOf(followUpPlanInbox.receivedAt))
            it.setTimestamp(8, followUpPlanInbox.validatedAt?.let(Timestamp::valueOf))
            it.setTimestamp(9, followUpPlanInbox.processedAt?.let(Timestamp::valueOf))
            it.setTimestamp(10, Timestamp.valueOf(followUpPlanInbox.updatedAt))
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.getFollowUpPlanInbox(correlationId: String): FollowUpPlanInbox? {
    val queryStatement =
        """
        SELECT *
        FROM FOLLOW_UP_PLAN_INBOX
        WHERE correlation_id = ?
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setString(1, correlationId)
            it.executeQuery().toNullableObject { toFollowUpPlanInbox() }
        }
    }
}

fun DatabaseInterface.updateFollowUpPlanInboxStatus(
    correlationId: String,
    status: InboxStatus,
    statusMessage: String? = null,
    validatedAt: LocalDateTime? = null,
    processedAt: LocalDateTime? = null,
): Int {
    val updateStatement =
        """
        UPDATE FOLLOW_UP_PLAN_INBOX
        SET status = ?, status_message = ?, validated_at = ?, processed_at = ?, updated_at = ?
        WHERE correlation_id = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setString(1, status.name)
                it.setString(2, statusMessage)
                it.setTimestamp(3, validatedAt?.let(Timestamp::valueOf))
                it.setTimestamp(4, processedAt?.let(Timestamp::valueOf))
                it.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()))
                it.setString(6, correlationId)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
    }
}

fun ResultSet.toFollowUpPlanInbox() =
    FollowUpPlanInbox(
        correlationId = getString("correlation_id"),
        organizationNumber = getString("organization_number"),
        lpsOrgnumber = getString("lps_orgnumber"),
        rawPayload = getString("raw_payload"),
        status = InboxStatus.valueOf(getString("status")),
        statusMessage = getString("status_message"),
        receivedAt = getTimestamp("received_at").toLocalDateTime(),
        validatedAt = getTimestamp("validated_at")?.toLocalDateTime(),
        processedAt = getTimestamp("processed_at")?.toLocalDateTime(),
        updatedAt = getTimestamp("updated_at").toLocalDateTime(),
    )
