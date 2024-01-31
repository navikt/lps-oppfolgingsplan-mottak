package no.nav.syfo.veileder

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.environment.isLocal

fun Routing.registerVeilederApi() {
    route("/api/internad/v3/oppfolgingsplan/lps") {
        val isLocal = isLocal()
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name, optional = isLocal) {
            get {
                call.respondText("OK")
            }
        }
    }
}
