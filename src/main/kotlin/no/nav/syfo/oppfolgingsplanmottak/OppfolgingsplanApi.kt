package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.database.storeLps
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanDTO
import java.util.*

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

    route("/api/v1/followupplan/write") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post {
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                database.storeFollowUpPlan(
                    uuid = UUID.randomUUID(),
                    followUpPlanDTO = followUpPlanDTO,
                    organizationNumber = "todo", //Skal hentes fra token claims (supplier ID)
                    lpsOrgnumber = "todo" //Skal hentes fra token claims (provider ID)
                )
                call.respondText("TODO")
            }
        }
    }
}

fun successText(virksomhetsnavn: String) =
    "Successfully received oppfolgingsplan for virksomhet $virksomhetsnavn"
