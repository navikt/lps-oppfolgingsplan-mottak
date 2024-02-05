package no.nav.syfo.application.api.auth

import no.nav.syfo.client.wellknown.WellKnown

data class MaskinportenJwtIssuer(
    val validScope: String,
    val jwtIssuerType: JwtIssuerType = JwtIssuerType.MASKINPORTEN,
    val wellKnown: WellKnown,
)

enum class JwtIssuerType {
    MASKINPORTEN,
}
