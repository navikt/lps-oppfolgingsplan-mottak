package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplanmottak.database.findSendingStatus
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.database.updateSentAt
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims
import no.nav.syfo.util.getSendingTimestamp
import org.slf4j.LoggerFactory
import java.util.*

fun Routing.registerFollowUpPlanApi(
    database: DatabaseInterface,
    followUpPlanSendingService: FollowUpPlanSendingService,
) {
    val log = LoggerFactory.getLogger("FollowUpPlanApi")
    val uuid = "uuid"

    route("/api/v1/followupplan") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post {
                try {
                    val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                    val planUuid = UUID.randomUUID()
                    val employerOrgnr = getOrgnumberFromClaims()
                    val lpsOrgnumber = getLpsOrgnumberFromClaims()

                    log.info("Received follow up plan from ${followUpPlanDTO.lpsName}, LPS orgnr: $lpsOrgnumber")

                    database.storeFollowUpPlan(
                        uuid = planUuid,
                        followUpPlanDTO = followUpPlanDTO,
                        organizationNumber = employerOrgnr,
                        lpsOrgnumber = lpsOrgnumber,
                        sentToGeneralPractitionerAt = null,
                        sentToNavAt = null,
                    )
                    val followUpPlan =
                        followUpPlanSendingService.sendFollowUpPlan(followUpPlanDTO, planUuid, employerOrgnr)

                    val sentToGeneralPractitionerAt =
                        getSendingTimestamp(followUpPlan.isSentToGeneralPractitionerStatus)
                    val sentToNavAt = getSendingTimestamp(followUpPlan.isSentToNavStatus)
                    val pdf = followUpPlan.pdf

                    database.updateSentAt(
                        planUuid,
                        sentToGeneralPractitionerAt = sentToGeneralPractitionerAt,
                        sentToNavAt = sentToNavAt,
                        pdf = pdf
                    )

                    call.respond(followUpPlan.toFollowUpPlanResponse())
                } catch (e: BadRequestException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid request. Error message: ${e.message}, Error cause: ${e.cause}"
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid input. Error message: ${e.message}, Error cause: ${e.cause}"
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Failed to retrieve follow-up plan: ${e.message}"
                    )
                }
            }

            get("/{$uuid}/sendingstatus") {
                val uuidString = call.parameters["uuid"].toString()
                val sendingStatus = database.findSendingStatus(UUID.fromString(uuidString))
                call.respond(sendingStatus)
            }
        }
    }
}
