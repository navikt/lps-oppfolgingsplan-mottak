package no.nav.syfo.api

import com.auth0.jwk.JwkProvider
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
    install(Authentication) {
        jwt(name = "maskinporten") {
            authHeader {
                if (it.getToken() == null) {
                    return@authHeader null
                }
                return@authHeader HttpAuthHeader.Single("Bearer", it.getToken()!!)
            }
            verifier(jwkProvider(env.auth.maskinporten.wellKnownUrl), env.auth.maskinporten.issuer)
            validate { credentials ->
                if (validScope(credentials, env.auth.maskinporten.scope)) {
                    JWTPrincipal(credentials.payload)
                } else {
                    null
                }
            }
        }
    }
}

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
