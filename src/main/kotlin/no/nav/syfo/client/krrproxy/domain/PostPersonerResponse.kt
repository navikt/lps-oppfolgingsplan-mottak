package no.nav.syfo.client.krrproxy.domain

data class PostPersonerResponse(
    val personer: Map<String, Kontaktinfo> = mapOf(),
    val feil: Map<String, String> = mapOf(),
)
