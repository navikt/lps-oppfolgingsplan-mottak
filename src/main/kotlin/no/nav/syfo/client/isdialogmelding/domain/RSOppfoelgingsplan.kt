package no.nav.syfo.client.isdialogmelding.domain

data class RSOppfoelgingsplan(
    val sykmeldtFnr: String,
    val oppfolgingsplanPdf: ByteArray,
)
