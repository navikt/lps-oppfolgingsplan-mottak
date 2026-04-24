package no.nav.syfo.oppfolgingsplanmottak.domain

import java.time.LocalDateTime

data class FollowUpPlanInbox(
    val correlationId: String,
    val organizationNumber: String,
    val lpsOrgnumber: String,
    val rawPayload: String,
    val receivedAt: LocalDateTime,
)
