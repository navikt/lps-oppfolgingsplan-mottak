package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDateTime
import java.util.*
import no.nav.syfo.altinnmottak.FollowUpPlanSendingService
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplanmottak.database.findSendingStatus
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims

fun Routing.registeFollowUpPlanApi(
    database: DatabaseInterface,
    followUpPlanSendingService: FollowUpPlanSendingService,
) {

    route("/api/v1/followupplan/") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post("write") {
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                val uuid = UUID.randomUUID()

                val followUpPlanResponse = followUpPlanSendingService.sendFollowUpPlan(followUpPlanDTO, uuid)
                val sentToGeneralPractitionerAt =
                    if ((followUpPlanResponse.isSentToGeneralPractitionerStatus != null) && followUpPlanResponse.isSentToGeneralPractitionerStatus != false) {
                        LocalDateTime.now()
                    } else {
                        null
                    }

                database.storeFollowUpPlan(
                    uuid = uuid,
                    followUpPlanDTO = followUpPlanDTO,
                    organizationNumber = getOrgnumberFromClaims(),
                    lpsOrgnumber = getLpsOrgnumberFromClaims(),
                    sentToGeneralPractitionerAt = sentToGeneralPractitionerAt,
                )

                call.respond(followUpPlanResponse)
            }

            get("read/sendingStatus/") {
                val uuid = call.parameters["uuid"].toString()

                val sendingStatus = database.findSendingStatus(UUID.fromString(uuid))
                call.respond(sendingStatus)
            }
        }
    }
}
