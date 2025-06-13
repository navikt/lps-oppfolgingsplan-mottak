package no.nav.syfo.application.api.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.request.header
import java.net.URI
import no.nav.syfo.application.exception.AuthenticationException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

private const val JWK_CACHE_SIZE = 10L
private const val JWK_CACHE_EXPIRES_IN = 24L
private const val JWK_BUCKET_SIZE = 10L
private const val JWK_REFILL_RATE = 1L

fun jwkProvider(jwksUri: String): JwkProvider =
    JwkProviderBuilder(URI(jwksUri).toURL()).cached(
        JWK_CACHE_SIZE,
        JWK_CACHE_EXPIRES_IN,
        TimeUnit.HOURS,
    )
        .rateLimited(JWK_BUCKET_SIZE, JWK_REFILL_RATE, TimeUnit.MINUTES).build()

fun claimsAreValid(credentials: JWTCredential, validIssuer: String, validScope: String) =
    isNotExpired(credentials) &&
        issuedBeforeExpiry(credentials) &&
        validIssuer(credentials, validIssuer) &&
        validScope(credentials, validScope) &&
        validConsumer(credentials)

fun isNotExpired(credentials: JWTCredential) =
    credentials.expiresAt?.after(Date()) ?: throw AuthenticationException("Missing iat-claim in JWT")

fun issuedBeforeExpiry(credentials: JWTCredential): Boolean {
    val expiredAt = credentials.expiresAt
    val issuedAt = credentials.issuedAt

    if (expiredAt == null || issuedAt == null) throw AuthenticationException("Missing exp or iat-claim in JWT")

    credentials.expiresAt?.after(credentials.issuedAt) ?: throw AuthenticationException("JWT is expired")

    return true
}

fun validIssuer(credentials: JWTCredential, validIssuer: String): Boolean {
    if (credentials.payload.issuer != validIssuer) {
        throw AuthenticationException("Invalid issuer in JWT")
    }
    return true
}

fun validScope(credentials: JWTCredential, validScope: String): Boolean {
    if (credentials.getClaim("scope", String::class) != validScope) {
        throw AuthenticationException("Invalid scope in JWT")
    }
    return true
}

fun validConsumer(credentials: JWTCredential): Boolean {
    val consumerClaim = credentials.payload.getClaim("consumer")
    if (consumerClaim?.asMap() == null) {
        throw throw AuthenticationException("Missing consumer claim in JWT")
    }
    return true
}

fun JWTCredential.inExpectedAudience(expectedAudience: List<String>) = expectedAudience.any {
    this.payload.audience.contains(it)
}

fun ApplicationCall.getToken() = request.header("Authorization")?.removePrefix("Bearer ")
