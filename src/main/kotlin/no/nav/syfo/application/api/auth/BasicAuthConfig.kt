package no.nav.syfo.application.api.auth

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import no.nav.syfo.application.environment.AuthBasic

fun AuthenticationConfig.configureBasicAuthentication(
    basicAuth: AuthBasic,
) {
    basic("test-token") {
        validate { credentials ->
            if (credentials.name == basicAuth.username && credentials.password == basicAuth.password) {
                return@validate UserIdPrincipal(credentials.name)
            }
            return@validate null
        }
    }
}
