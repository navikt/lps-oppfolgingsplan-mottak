package no.nav.syfo.api.lps

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.environment.isLocal

fun Routing.registerOppfolgingsplanApi() {
    route("/api/v1/lps/write") {
        val isLocal = isLocal()
        authenticate("maskinporten", optional = isLocal) {
            get {
                call.respondText("Called GET /api/v1/lps/write")
            }
            post {
                val oppfolgingsplanDTO = call.receive<OppfolgingsplanDTO>()
                call.respondText(
                    "Recieved oppfolgingsplan for virksomhet " +
                            oppfolgingsplanDTO.oppfolgingsplanMeta.virksomhet.virksomhetsnavn
                )
            }
        }
    }
}
