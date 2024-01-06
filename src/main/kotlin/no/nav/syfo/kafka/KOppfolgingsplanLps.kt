package no.nav.syfo.kafka

data class KOppfolgingsplanLps(
    val uuid: String,
    val fodselsnummer: String,
    val virksomhetsnummer: String,
    val behovForBistandFraNav: Boolean,
    val opprettet: Int
)
