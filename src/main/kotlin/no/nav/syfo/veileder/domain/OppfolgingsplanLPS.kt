package no.nav.syfo.veileder.domain

import java.time.LocalDateTime
import java.util.UUID

data class OppfolgingsplanLPS(
    val uuid: UUID,
    val fnr: String,
    val virksomhetsnummer: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
)
