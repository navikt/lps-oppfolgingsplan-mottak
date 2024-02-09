package no.nav.syfo.veileder

import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.api.VeilederTilgangskontrollPlugin
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient

const val VEILEDER_LPS_METADATA_PATH = "/api/internad/v1/oppfolgingsplan/lps"
private const val API_ACTION = "access lps plan for person"

fun Routing.registerVeilederApi(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
) {
    route(VEILEDER_LPS_METADATA_PATH) {
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            install(VeilederTilgangskontrollPlugin) {
                this.action = API_ACTION
                this.veilederTilgangskontrollClient = veilederTilgangskontrollClient
            }
            get {
                call.respondText("OK")
            }
        }
    }
}
