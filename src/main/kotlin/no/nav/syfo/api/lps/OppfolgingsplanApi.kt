package no.nav.syfo.api.lps

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.consumer.isdialogmelding.IsdialogmeldingConsumer
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.storeLps
import no.nav.syfo.environment.isLocal
import no.nav.syfo.service.LpsOppfolgingsplanSendingService

fun Routing.registerOppfolgingsplanApi(
    database: DatabaseInterface,
    isdialogmeldingConsumer: IsdialogmeldingConsumer,
    lpsOppfolgingsplanSendingService: LpsOppfolgingsplanSendingService,
) {
    route("/api/v1/lps") {
        val isLocal = isLocal()
        authenticate("maskinporten", optional = isLocal) {
            post("/write") {
                val oppfolgingsplanDTO = call.receive<OppfolgingsplanDTO>()
                database.storeLps(oppfolgingsplanDTO, 1)
                val lpsOppfolgingsplan = lpsOppfolgingsplanSendingService.sendLpsPlan(oppfolgingsplanDTO)

                call.respond(lpsOppfolgingsplan)
            }

            get("/status/delt/fastlege") {
                val bestillingsUuid = call.parameters["sentToFastlegeId"].toString()
                val delingsstatus = isdialogmeldingConsumer.getDeltMedFastlegeStatus(bestillingsUuid)
                if (delingsstatus != null) {
                    call.respond(delingsstatus)
                } else {
                    call.respond(status = HttpStatusCode.NotFound, message = "Error while fetching sending to fastlege status")
                }
            }
        }
    }
}
