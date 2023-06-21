package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.environment.Environment
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

fun Application.setupAuth(
    env: Environment
) {
    val maskinportenAuth = env.auth.maskinporten
    val basicAuth = env.auth.basic

    install(Authentication) {
        jwt(name = "maskinporten") {
            authHeader {
                if (it.getToken() == null) {
                    return@authHeader null
                }
                return@authHeader HttpAuthHeader.Single("Bearer", it.getToken()!!)
            }
            verifier(jwkProvider(maskinportenAuth.wellKnownUrl), maskinportenAuth.issuer)
            validate { credentials ->
                if (claimsAreValid(credentials, maskinportenAuth.issuer, maskinportenAuth.scope)) {
                    return@validate JWTPrincipal(credentials.payload)
                }
                return@validate null
            }
        }

        basic("test-token") {
            validate { credentials ->
                if (credentials.name == basicAuth.username && credentials.password == basicAuth.password) {
                    return@validate UserIdPrincipal(credentials.name)
                }
                return@validate null
            }
        }
    }
}

private fun claimsAreValid(credentials: JWTCredential, validIssuer: String, validScope: String) =
        isNotExpired(credentials) &&
            issuedBeforeExpiry(credentials) &&
            validIssuer(credentials, validIssuer) &&
            validScope(credentials, validScope)

private fun isNotExpired(credentials: JWTCredential) =
    credentials.expiresAt?.after(Date()) ?: throw RuntimeException("Missing iat-claim in JWT")

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

private fun getJwksUriFromWellKnown(wellKnownUri: String): String {
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

private fun jwkProvider(wellKnownUri: String) =
    JwkProviderBuilder(URL(getJwksUriFromWellKnown(wellKnownUri)))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

private fun ApplicationCall.getToken() =
    request.header("Authorization")?.removePrefix("Bearer ")

@JsonIgnoreProperties(ignoreUnknown = true)
data class MaskinportenWellKnown(
    val jwks_uri: String,
    val issuer: String
)
