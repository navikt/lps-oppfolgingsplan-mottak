package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplanmottak.database.storeLps
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanDTO

fun Routing.registerOppfolgingsplanApi(
    database: DatabaseInterface,
) {
    route("/api/v1/lps/write") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post {
                val oppfolgingsplanDTO = call.receive<OppfolgingsplanDTO>()
                val virksomhetsnavn = oppfolgingsplanDTO.oppfolgingsplanMeta.virksomhet.virksomhetsnavn
                database.storeLps(oppfolgingsplanDTO, 1)
                call.respondText(successText(virksomhetsnavn))
            }
        }
    }
}

fun successText(virksomhetsnavn: String) =
    "Successfully received oppfolgingsplan for virksomhet $virksomhetsnavn"
