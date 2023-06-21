package no.nav.syfo.environment

data class AuthEnv(
    val maskinporten: AuthMaskinporten,
    val basic: AuthBasic
)
data class AuthMaskinporten(
    val wellKnownUrl: String,
    val issuer: String,
    val scope: String,
    val tokenUrl: String,
    val clientId: String,
    val clientJwk: String
)

data class AuthBasic(
    val username: String,
    val password: String
)
