package no.nav.syfo.client.wellknown

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class WellKnownDTO(
    val issuer: String,
    val jwks_uri: String,
)

fun WellKnownDTO.toWellKnown() =
    WellKnown(
        issuer = this.issuer,
        jwksUri = this.jwks_uri,
    )
