package no.nav.syfo.environment

data class UrlEnv(
    val pdlUrl: String,
    val pdlScope: String,
    val opPdfGenUrl: String,
    val isdialogmeldingUrl: String,
    val isdialogmeldingClientId: String,
    val dokarkivUrl: String,
    val dokarkivScope: String,
)
