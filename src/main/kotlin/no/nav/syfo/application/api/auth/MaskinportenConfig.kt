package no.nav.syfo.application.api.auth

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun AuthenticationConfig.configureMaskinportenJwt(
    jwtIssuer: MaskinportenJwtIssuer,
) {
    jwt(name = jwtIssuer.jwtIssuerType.name) {
        authHeader {
            val token = it.getToken()
            if (token == null) {
                return@authHeader null
            }
            return@authHeader HttpAuthHeader.Single("Bearer", token)
        }
        verifier(jwkProvider(jwtIssuer.wellKnown.jwksUri), jwtIssuer.wellKnown.issuer)
        validate { credentials ->
            if (claimsAreValid(credentials, jwtIssuer.wellKnown.issuer, jwtIssuer.validScope)) {
                return@validate JWTPrincipal(credentials.payload)
            }
            return@validate null
        }
    }
}
