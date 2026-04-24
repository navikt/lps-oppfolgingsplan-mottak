package no.nav.syfo.oppfolgingsplanmottak.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toNullableObject
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanInbox
import java.sql.ResultSet
import java.sql.Timestamp

fun DatabaseInterface.storeFollowUpPlanInbox(followUpPlanInbox: FollowUpPlanInbox) {
    val insertStatement =
        """
        INSERT INTO FOLLOW_UP_PLAN_INBOX (
            correlation_id,
            organization_number,
            lps_orgnumber,
            raw_payload,
            received_at
        ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setString(1, followUpPlanInbox.correlationId)
            it.setString(2, followUpPlanInbox.organizationNumber)
            it.setString(3, followUpPlanInbox.lpsOrgnumber)
            it.setString(4, followUpPlanInbox.rawPayload)
            it.setTimestamp(5, Timestamp.valueOf(followUpPlanInbox.receivedAt))
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

fun DatabaseInterface.getLatestFollowUpPlanInbox(): FollowUpPlanInbox? {
    val queryStatement =
        """
        SELECT *
        FROM FOLLOW_UP_PLAN_INBOX
        ORDER BY received_at DESC
        LIMIT 1
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.executeQuery().toNullableObject { toFollowUpPlanInbox() }
        }
    }
}

fun ResultSet.toFollowUpPlanInbox() =
    FollowUpPlanInbox(
        correlationId = getString("correlation_id"),
        organizationNumber = getString("organization_number"),
        lpsOrgnumber = getString("lps_orgnumber"),
        rawPayload = getString("raw_payload"),
        receivedAt = getTimestamp("received_at").toLocalDateTime(),
    )
