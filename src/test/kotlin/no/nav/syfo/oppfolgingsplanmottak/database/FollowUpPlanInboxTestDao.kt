package no.nav.syfo.oppfolgingsplanmottak.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toNullableObject

fun DatabaseInterface.getLatestFollowUpPlanInbox() =
    connection.use { connection ->
        connection
            .prepareStatement(
                """
                SELECT *
                FROM FOLLOW_UP_PLAN_INBOX
                ORDER BY received_at DESC
                LIMIT 1
                """.trimIndent(),
            ).use {
                it.executeQuery().toNullableObject { toFollowUpPlanInbox() }
            }
    }

fun DatabaseInterface.countFollowUpPlanInboxRows() =
    connection.use { connection ->
        connection
            .prepareStatement(
                """
                SELECT COUNT(*) AS count
                FROM FOLLOW_UP_PLAN_INBOX
                """.trimIndent(),
            ).use {
                it.executeQuery().toNullableObject { getInt("count") }
            } ?: 0
    }
