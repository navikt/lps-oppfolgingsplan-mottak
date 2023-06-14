package no.nav.syfo.api.lps

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.registerOppfolgingsplanApi() {
    route("/api/v1/lps/write") {
        authenticate("maskinporten") {
            get {
                call.respondText("Called GET /api/v1/lps/write")
            }
            post {
                call.respondText("Called POST /api/v1/lps/write")
            }
        }
    }
}
