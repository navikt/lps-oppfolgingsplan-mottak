package no.nav.syfo.application.api.auth

import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.syfo.application.exception.AuthenticationException

fun AuthenticationConfig.configureMaskinportenJwt(
    jwtIssuer: MaskinportenJwtIssuer,
) {
    jwt(name = jwtIssuer.jwtIssuerType.name) {
        authHeader {
            val token = it.getToken() ?: return@authHeader null
            return@authHeader HttpAuthHeader.Single("Bearer", token)
        }
        verifier(jwkProvider(jwtIssuer.wellKnown.jwksUri), jwtIssuer.wellKnown.issuer)
        validate { credentials ->
            try {
                if (claimsAreValid(credentials, jwtIssuer.wellKnown.issuer, jwtIssuer.validScope)) {
                    return@validate JWTPrincipal(credentials.payload)
                }
            } catch (e: AuthenticationException) {
                this.attributes.put(AttributeKey("auth_error"), e.message ?: "Authentication failed")
                return@validate null
            }
            return@validate null
        }
        challenge { _, _ ->
            val authError = call.attributes[AttributeKey<String>("auth_error")]
            call.respond(HttpStatusCode.Unauthorized, authError)
        }
    }
}
