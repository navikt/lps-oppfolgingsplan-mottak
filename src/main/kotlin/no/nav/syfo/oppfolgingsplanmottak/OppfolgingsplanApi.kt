package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.altinnmottak.FollowUpPlanSendingService
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.database.findSendingStatus
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

fun Routing.registeFollowUpPlanApi(
    database: DatabaseInterface,
    followUpPlanSendingService: FollowUpPlanSendingService,
) {
    val log = LoggerFactory.getLogger("registeFollowUpPlanApi")

    route("/api/v1/followupplan/") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post("write") {
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                val uuid = UUID.randomUUID()

                val followUpPlanResponse = followUpPlanSendingService.sendFollowUpPlan(followUpPlanDTO, uuid)

                database.storeFollowUpPlan(
                    uuid = uuid,
                    followUpPlanDTO = followUpPlanDTO,
                    organizationNumber = getOrgnumberFromClaims(),
                    lpsOrgnumber = getLpsOrgnumberFromClaims(),
                    sentToGeneralPractitionerAt = if ((followUpPlanResponse.sentToGeneralPractitionerStatus != null) && followUpPlanResponse.sentToGeneralPractitionerStatus) LocalDateTime.now() else null,
                )

                call.respond(followUpPlanResponse)
            }

            get("read/status/delt/fastlege") {
                val uuid = call.parameters["uuid"].toString()

                val sendingStatus = database.findSendingStatus(UUID.fromString(uuid))
                call.respond(sendingStatus)
            }
        }
    }
}
