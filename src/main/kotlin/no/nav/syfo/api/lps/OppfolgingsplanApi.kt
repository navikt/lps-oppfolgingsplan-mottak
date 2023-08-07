package no.nav.syfo.api.lps

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.api.lps.dto.OppfolgingsplanDTO
import no.nav.syfo.api.lps.dto.OppfolgingsplanPdfDTO
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.storeLps
import no.nav.syfo.environment.isLocal

fun Routing.registerOppfolgingsplanApi(
    database: DatabaseInterface,
) {
    route("/api/v1/lps/write") {
        val isLocal = isLocal()
        authenticate("maskinporten", optional = isLocal) {
            post {
                val oppfolgingsplanDTO = call.receive<OppfolgingsplanDTO>()
                val virksomhetsnavn = oppfolgingsplanDTO.oppfolgingsplanMeta.virksomhet.virksomhetsnavn
                database.storeLps(oppfolgingsplanDTO, 1)
                call.respondText(successText(virksomhetsnavn))
            }
        }
    }

    route("/api/v1/lps/pdfplan") {
        val isLocal = isLocal()
        authenticate("maskinporten", optional = isLocal) {
            post {
                val oppfolgingsplanDTO = call.receive<OppfolgingsplanPdfDTO>()
                val virksomhetsnavn = oppfolgingsplanDTO.oppfolgingsplanMeta.virksomhet.virksomhetsnavn
//                database.storeLps(oppfolgingsplanDTO, 1)
                call.respondText(successText(virksomhetsnavn))
            }
        }
    }
}

fun successText(virksomhetsnavn: String) =
        "Successfully received oppfolgingsplan for virksomhet $virksomhetsnavn"
