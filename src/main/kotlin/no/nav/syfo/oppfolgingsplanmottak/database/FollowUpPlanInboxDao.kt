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
            employee_identification_number,
            raw_payload,
            received_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setString(1, followUpPlanInbox.correlationId)
            it.setString(2, followUpPlanInbox.organizationNumber)
            it.setString(3, followUpPlanInbox.lpsOrgnumber)
            it.setString(4, followUpPlanInbox.employeeIdentificationNumber)
            it.setString(5, followUpPlanInbox.rawPayload)
            it.setTimestamp(6, Timestamp.valueOf(followUpPlanInbox.receivedAt))
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

fun DatabaseInterface.deleteFollowUpPlanInboxRowsOlderThan14Days(): Int {
    val deleteStatement =
        """
        DELETE FROM FOLLOW_UP_PLAN_INBOX
        WHERE received_at < CURRENT_TIMESTAMP - INTERVAL '14 days'
        """.trimIndent()

    return connection.use { connection ->
        val deletedRows =
            connection.prepareStatement(deleteStatement).use {
                it.executeUpdate()
            }
        connection.commit()
        deletedRows
    }
}

fun ResultSet.toFollowUpPlanInbox() =
    FollowUpPlanInbox(
        correlationId = getString("correlation_id"),
        organizationNumber = getString("organization_number"),
        lpsOrgnumber = getString("lps_orgnumber"),
        employeeIdentificationNumber = getString("employee_identification_number"),
        rawPayload = getString("raw_payload"),
        receivedAt = getTimestamp("received_at").toLocalDateTime(),
    )
