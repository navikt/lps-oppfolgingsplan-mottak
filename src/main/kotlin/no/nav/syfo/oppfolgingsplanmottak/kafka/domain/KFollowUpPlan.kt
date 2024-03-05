package no.nav.syfo.altinnmottak.kafka.domain

data class KFollowUpPlan(
    val uuid: String,
    val fodselsnummer: String,
    val virksomhetsnummer: String,
    val behovForBistandFraNav: Boolean,
    val opprettet: Int
)
