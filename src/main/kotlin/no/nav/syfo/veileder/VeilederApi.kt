package no.nav.syfo.veileder

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.api.auth.JwtIssuerType

const val VEILEDER_LPS_METADATA_PATH = "/api/internad/v1/oppfolgingsplan/lps"

fun Routing.registerVeilederApi() {
    route(VEILEDER_LPS_METADATA_PATH) {
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            get {
                call.respondText("OK")
            }
        }
    }
}
