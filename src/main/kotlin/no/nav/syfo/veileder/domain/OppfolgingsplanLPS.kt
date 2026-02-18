package no.nav.syfo.veileder.domain

import java.time.LocalDateTime
import java.util.*

data class OppfolgingsplanLPS(
    val uuid: UUID,
    val fnr: String,
    val virksomhetsnummer: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
)
