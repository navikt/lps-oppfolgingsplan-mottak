package no.nav.syfo.oppfolgingsplanmottak.domain

import java.time.LocalDateTime

data class FollowUpPlanInbox(
    val correlationId: String,
    val organizationNumber: String,
    val lpsOrgnumber: String,
    val rawPayload: String,
    val status: InboxStatus,
    val statusMessage: String?,
    val receivedAt: LocalDateTime,
    val validatedAt: LocalDateTime?,
    val processedAt: LocalDateTime?,
    val updatedAt: LocalDateTime,
)
