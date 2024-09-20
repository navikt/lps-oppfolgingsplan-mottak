package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.exception.ApiError.FollowupPlanNotFoundError
import no.nav.syfo.application.metric.COUNT_METRIKK_PROSSESERING_FOLLOWUP_LPS_PROSSESERING_VELLYKKET
import no.nav.syfo.oppfolgingsplanmottak.database.findSendingStatus
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.database.updateSentAt
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.oppfolgingsplanmottak.validation.FollowUpPlanValidator
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims
import no.nav.syfo.util.getSendingTimestamp
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerFollowUpPlanApi(
    database: DatabaseInterface,
    followUpPlanSendingService: FollowUpPlanSendingService,
    validator: FollowUpPlanValidator,
) {
    val log = LoggerFactory.getLogger("FollowUpPlanApi")
    val uuid = "uuid"

    route("/api/v1/followupplan") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post {
                log.info("Received follow-up plan")
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                val planUuid = UUID.randomUUID()
                val employerOrgnr = getOrgnumberFromClaims()
                val lpsOrgnumber = getLpsOrgnumberFromClaims() ?: employerOrgnr

                validator.validateFollowUpPlanDTO(followUpPlanDTO, employerOrgnr)
                log.info("Follow-up plan is valid. Attempting to store plan.")

                database.storeFollowUpPlan(
                    uuid = planUuid,
                    followUpPlanDTO = followUpPlanDTO,
                    organizationNumber = employerOrgnr,
                    lpsOrgnumber = lpsOrgnumber,
                    sentToGeneralPractitionerAt = null,
                    sentToNavAt = null,
                )

                log.info("Follow-up plan stored successfully. Attempting to send follow-up plan.")

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

                log.info("Follow-up plan received and sent successfully.")
                call.respond(followUpPlan.toFollowUpPlanResponse())

                COUNT_METRIKK_PROSSESERING_FOLLOWUP_LPS_PROSSESERING_VELLYKKET.increment()
            }

            get("/{$uuid}/sendingstatus") {
                val sendingStatus = database.findSendingStatus(call.uuid())
                if (sendingStatus != null) {
                    call.respond(sendingStatus)
                } else {
                    call.respond(HttpStatusCode.NotFound, FollowupPlanNotFoundError)
                }
            }

            get("/verify-integration") {
                call.respond(HttpStatusCode.OK, "Integration is up and running")
            }
        }
    }
}

private fun ApplicationCall.uuid(): UUID =
    UUID.fromString(this.parameters["uuid"])
        ?: throw IllegalArgumentException(
            "Failed to fetch follow-up plan sending status: No valid follow-up plan uuid supplied in request"
        )
