package no.nav.syfo.environment

data class AuthEnv(
    val maskinporten: AuthMaskinporten
)
data class AuthMaskinporten(
    val wellKnownUrl: String,
    val scope: String
)
