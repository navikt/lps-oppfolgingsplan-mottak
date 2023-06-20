package no.nav.syfo.environment

data class AuthEnv(
    val maskinporten: AuthMaskinporten
)
data class AuthMaskinporten(
    val wellKnownUrl: String,
    val issuer: String,
    val scope: String,
    val tokenUrl: String,
    val clientId: String,
    val clientJwk: String
)
