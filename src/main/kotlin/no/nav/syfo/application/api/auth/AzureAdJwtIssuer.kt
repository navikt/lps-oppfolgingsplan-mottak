package no.nav.syfo.application.api.auth

import no.nav.syfo.client.wellknown.WellKnown

data class AzureAdJwtIssuer(
    val acceptedAudienceList: List<String>,
    val jwtIssuerType: JwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
    val wellKnown: WellKnown,
)

data class MaskinportenJwtIssuer(
    val validScope: String,
    val jwtIssuerType: JwtIssuerType = JwtIssuerType.MASKINPORTEN,
    val wellKnown: WellKnown,
)

enum class JwtIssuerType {
    INTERNAL_AZUREAD,
    MASKINPORTEN,
}
