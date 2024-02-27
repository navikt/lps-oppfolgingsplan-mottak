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
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.database.storeLps
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims
import java.util.*
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanDTO
import org.slf4j.LoggerFactory

fun Routing.registerOppfolgingsplanApi(
    database: DatabaseInterface,
    isdialogmeldingClient: IsdialogmeldingClient,
    lpsOppfolgingsplanSendingService: LpsOppfolgingsplanSendingService,
) {
    val log = LoggerFactory.getLogger("registerOppfolgingsplanApi")

    route("/api/v1/followupplan/") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post("write") {
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                val uuid = UUID.randomUUID()

                database.storeFollowUpPlan(
                    uuid = uuid,
                    followUpPlanDTO = followUpPlanDTO,
                    organizationNumber = getOrgnumberFromClaims(),
                    lpsOrgnumber = getLpsOrgnumberFromClaims()
                )
//         TODO      val lpsPlan = lpsOppfolgingsplanSendingService.sendLpsPlan(followUpPlanDTO)
//                call.respond(lpsPlan)

                call.respond(FollowUpPlanResponse(uuid.toString()))
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
