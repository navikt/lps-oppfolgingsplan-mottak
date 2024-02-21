package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.altinnmottak.LpsOppfolgingsplanSendingService
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.database.storeLps
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanDTO
import org.slf4j.LoggerFactory

fun Routing.registerOppfolgingsplanApi(
    database: DatabaseInterface,
    isdialogmeldingClient: IsdialogmeldingClient,
    lpsOppfolgingsplanSendingService: LpsOppfolgingsplanSendingService,
) {
    val log = LoggerFactory.getLogger("registerOppfolgingsplanApi")

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

    route("/api/v1/followupplan/") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post("write") {
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                call.respondText(successText("Hello world"))
            }
        }
    }

    route("/api/v2/followupplan/") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post("write") {
                log.warn("About to send a plan")
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                val lpsPlan = lpsOppfolgingsplanSendingService.sendLpsPlan(followUpPlanDTO)
                call.respond(lpsPlan)
            }
            get("read/status/delt/fastlege") {
                val bestillingsUuid = call.parameters["sentToFastlegeId"].toString()
                val delingsstatus = isdialogmeldingClient.getDeltMedFastlegeStatus(bestillingsUuid)
                if (delingsstatus != null) {
                    call.respond(delingsstatus)
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Error while fetching sending to fastlege status")
                }
            }
        }
    }
}

fun successText(virksomhetsnavn: String) =
    "Successfully received oppfolgingsplan for virksomhet $virksomhetsnavn"
