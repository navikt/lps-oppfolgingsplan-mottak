package no.nav.syfo.oppfolgingsplanmottak.domain

enum class InboxStatus {
    RECEIVED,
    VALIDATED,
    REJECTED,
    PROCESSED,
    VALIDATED_TECHNICAL_FAILURE,
}
