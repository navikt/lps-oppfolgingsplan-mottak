package no.nav.syfo.application.environment

data class AuthEnv(
    val maskinporten: AuthMaskinporten,
    val basic: AuthBasic,
    val azuread: AzureAd,
)

data class AuthMaskinporten(
    val wellKnownUrl: String,
    val issuer: String,
    val scope: String,
    val tokenUrl: String,
    val clientId: String,
    val clientJwk: String,
)

data class AuthBasic(
    val username: String,
    val password: String,
)

data class AzureAd(
    val clientId: String,
    val clientSecret: String,
    val accessTokenUrl: String,
    val wellKnownUrl: String,
)
