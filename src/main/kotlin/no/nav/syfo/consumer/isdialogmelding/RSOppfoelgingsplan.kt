package no.nav.syfo.consumer.isdialogmelding

data class RSOppfoelgingsplan(
    val sykmeldtFnr: String,
    val oppfolgingsplanPdf: ByteArray,
)
