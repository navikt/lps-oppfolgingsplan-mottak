package no.nav.syfo.application.api.auth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.syfo.application.ApplicationEnvironment

fun Application.installAuthentication(
    env: ApplicationEnvironment
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

@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class MaskinportenWellKnown(
    val jwks_uri: String,
    val issuer: String
)
