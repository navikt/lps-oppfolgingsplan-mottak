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
import org.slf4j.LoggerFactory

fun AuthenticationConfig.configureMaskinportenJwt(
    jwtIssuer: MaskinportenJwtIssuer,
) {
    val defaultErrorMessage = "Authentication failed. Please check your token"
    val log = LoggerFactory.getLogger(AuthenticationConfig::class.java)
    log.info("Validating JWT with issuer: ${jwtIssuer.jwtIssuerType.name}")

    jwt(name = jwtIssuer.jwtIssuerType.name) {
        authHeader {
            val token = it.getToken() ?: return@authHeader null
            log.info("Received token: $token")
            return@authHeader HttpAuthHeader.Single("Bearer", token)
        }
        val jwkProvider = jwkProvider(jwtIssuer.wellKnown.jwksUri)
        val issuer = jwtIssuer.wellKnown.issuer

        log.info("Configuring JWT verifier with JWKS URI: ${jwtIssuer.wellKnown.jwksUri} and issuer: $issuer")
        verifier(jwkProvider, issuer)
        log.info("JWT verifier configured successfully")

        validate { credentials ->
            log.info("Validating JWT credentials")
            try {
                if (claimsAreValid(credentials, jwtIssuer.wellKnown.issuer, jwtIssuer.validScope)) {
                    log.info("JWT claims are valid")
                    return@validate JWTPrincipal(credentials.payload)
                } else {
                    log.warn("JWT claims are invalid")
                }
            } catch (e: AuthenticationException) {
                log.error("AuthenticationException occurred: ${e.message}")
                this.attributes.put(AttributeKey("auth_error"), e.message ?: defaultErrorMessage)
                return@validate null
            }

            log.warn("JWT validation failed")
            return@validate null
        }
        challenge { _, _ ->
            val authError = call.attributes.getOrNull(AttributeKey<String>("auth_error")) ?: defaultErrorMessage
            log.warn("Authentication challenge triggered: $authError")
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError.AuthenticationError(
                    message = authError
                )
            )
        }
    }
}