package no.nav.syfo.api

import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.syfo.environment.Environment

fun Application.setupAuth(
    env: Environment
) {
    install(Authentication) {
        jwt(name = "maskinporten") {
            validate { credentials ->
                if (validIssuer(credentials, env.auth.maskinporten.issuer)) {
                    return@validate JWTPrincipal(credentials.payload)
                }
                return@validate null
            }
        }
    }
}

private fun validIssuer(credentials: JWTCredential, validIssuer: String): Boolean {
    return credentials.payload.issuer == validIssuer
}
