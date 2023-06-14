package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.environment.Environment
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

fun Application.setupAuth(
    env: Environment
) {
    val wellKnown = getWellKnownMetadata(env.auth.maskinporten.wellKnownUrl)

    install(Authentication) {
        jwt(name = "maskinporten") {
            verifier(
                JwkProviderBuilder(URL(wellKnown.jwks_uri))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build(),
                wellKnown.issuer)
            validate { credentials ->
                if (claimsAreValid(credentials, wellKnown.issuer, env.auth.maskinporten.scope)) {
                    return@validate JWTPrincipal(credentials.payload)
                }
                return@validate null
            }
        }
    }
}

private fun getWellKnownMetadata(wellKnownUri: String): MaskinportenWellKnown {
    val client = httpClient()
    return runBlocking {
        val response = client.get(wellKnownUri) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
        response.body<MaskinportenWellKnown>()
    }
}

private fun claimsAreValid(credentials: JWTCredential, validIssuer: String, validScope: String) =
    isNotExpired(credentials) &&
    issuedBeforeExpiry(credentials) &&
    validIssuer(credentials, validIssuer) &&
    validScope(credentials, validScope)

private fun isNotExpired(credentials: JWTCredential) =
    credentials.expiresAt?.before(Date()) ?: throw RuntimeException("Missing iat-claim in JWT")

private fun issuedBeforeExpiry(credentials: JWTCredential): Boolean {
    val expiredAt = credentials.expiresAt
    val issuedAt = credentials.issuedAt

    if (expiredAt == null || issuedAt == null)
        throw RuntimeException("Missing exp or iat-claim in JWT")

    return credentials.expiresAt?.after(credentials.issuedAt) ?: false
}

private fun validIssuer(credentials: JWTCredential, validIssuer: String) =
    credentials.payload.issuer == validIssuer

private fun validScope(credentials: JWTCredential, validScope: String) =
    credentials.getClaim("scope", String::class) == validScope

@JsonIgnoreProperties(ignoreUnknown = true)
data class MaskinportenWellKnown(
    val jwks_uri: String,
    val issuer: String
)
