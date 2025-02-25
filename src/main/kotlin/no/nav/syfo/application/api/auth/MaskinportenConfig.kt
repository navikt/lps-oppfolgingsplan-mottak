package no.nav.syfo.application.api.auth

import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.syfo.application.exception.ApiError
import no.nav.syfo.application.exception.AuthenticationException

fun AuthenticationConfig.configureMaskinportenJwt(
    jwtIssuer: MaskinportenJwtIssuer,
) {
    val defaultErrorMessage = "Authentication failed. Please check your token"

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
                this.attributes.put(AttributeKey("auth_error"), e.message ?: defaultErrorMessage)
                return@validate null
            } catch (e: Exception) {
                this.attributes.put(AttributeKey("auth_error"), "Token verification failed: ${e.message}")
                return@validate null
            }

            return@validate null
        }
        challenge { _, _ ->
            val authError = call.attributes.getOrNull(AttributeKey<String>("auth_error")) ?: defaultErrorMessage
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError.AuthenticationError(
                    message = authError
                )
            )
        }
    }
}
