package no.nav.syfo.maskinporten

@Suppress("ConstructorParameterNaming")
data class MaskinportenResponse(
    val access_token: String?,
    val error: String?,
    val error_description: String?
)
