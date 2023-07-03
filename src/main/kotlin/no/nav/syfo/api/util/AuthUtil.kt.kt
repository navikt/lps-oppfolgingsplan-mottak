package no.nav.syfo.api.util

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.MaskinportenWellKnown
import no.nav.syfo.exception.AuthenticationException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

private const val JWK_CACHE_SIZE = 10L
private const val JWK_CACHE_EXPIRES_IN = 24L
private const val JWK_BUCKET_SIZE = 10L
private const val JWK_REFILL_RATE = 1L

fun getJwksUriFromWellKnown(wellKnownUri: String): String {
    val client = httpClient()
    return runBlocking {
        val response = client.get(wellKnownUri) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
        response.body<MaskinportenWellKnown>().jwks_uri
    }
}

fun jwkProvider(wellKnownUri: String): JwkProvider =
    JwkProviderBuilder(URL(getJwksUriFromWellKnown(wellKnownUri))).cached(
        JWK_CACHE_SIZE,
        JWK_CACHE_EXPIRES_IN,
        TimeUnit.HOURS
    )
        .rateLimited(JWK_BUCKET_SIZE, JWK_REFILL_RATE, TimeUnit.MINUTES).build()

fun claimsAreValid(credentials: JWTCredential, validIssuer: String, validScope: String) =
    isNotExpired(credentials) && issuedBeforeExpiry(credentials) && validIssuer(credentials, validIssuer) && validScope(
        credentials, validScope
    )

fun isNotExpired(credentials: JWTCredential) =
    credentials.expiresAt?.after(Date()) ?: throw AuthenticationException("Missing iat-claim in JWT")

fun issuedBeforeExpiry(credentials: JWTCredential): Boolean {
    val expiredAt = credentials.expiresAt
    val issuedAt = credentials.issuedAt

    if (expiredAt == null || issuedAt == null) throw AuthenticationException("Missing exp or iat-claim in JWT")

    return credentials.expiresAt?.after(credentials.issuedAt) ?: false
}

fun validIssuer(credentials: JWTCredential, validIssuer: String) = credentials.payload.issuer == validIssuer

fun validScope(credentials: JWTCredential, validScope: String) =
    credentials.getClaim("scope", String::class) == validScope

fun ApplicationCall.getToken() = request.header("Authorization")?.removePrefix("Bearer ")
