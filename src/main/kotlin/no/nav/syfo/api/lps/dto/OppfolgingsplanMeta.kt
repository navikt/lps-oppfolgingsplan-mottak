package no.nav.syfo.api.lps.dto

import java.time.LocalDateTime

data class OppfolgingsplanMeta(
    val utfyllingsdato: LocalDateTime,
    val mottaker: Mottaker,
    val sykmeldtFnr: String,
    val virksomhet: Virksomhet,
)

data class Virksomhet(
    val virksomhetsnavn: String,
    val virksomhetsnummer: String,
    val naermesteLederFornavn: String,
    val naermesteLederEtternavn: String,
    val telefonNaermesteLeder: String,
)

enum class Mottaker {
    NAV, FASTLEGE, NAVOGFASTLEGE
}
