package no.nav.syfo.oppfolgingsplanmottak.kafka.domain

data class KFollowUpPlan(
    val uuid: String,
    val fodselsnummer: String,
    val virksomhetsnummer: String,
    val behovForBistandFraNav: Boolean,
    val opprettet: Long
)
