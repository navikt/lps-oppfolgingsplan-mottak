package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.*
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.getEnvVar
import no.nav.syfo.application.metric.COUNT_METRIKK_PROSSESERING_FOLLOWUP_LPS_PROSSESERING_VELLYKKET
import no.nav.syfo.oppfolgingsplanmottak.database.findSendingStatus
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.database.updateSentAt
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims
import no.nav.syfo.util.getSendingTimestamp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.ParseException
import java.text.SimpleDateFormat

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

                    validateEmployeeIdentificationNumber(followUpPlanDTO.employeeIdentificationNumber, log)

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
                COUNT_METRIKK_PROSSESERING_FOLLOWUP_LPS_PROSSESERING_VELLYKKET.increment()
            }

            get("/{$uuid}/sendingstatus") {
                try {
                    val sendingStatus = database.findSendingStatus(call.uuid())
                    if (sendingStatus != null){
                        call.respond(sendingStatus)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "The follow-up plan with a given uuid was not found"
                        )
                    }
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
                        "Failed to fetch follow-up plan status: ${e.message}"
                    )
                }
            }
        }
    }
}

private fun ApplicationCall.uuid(): UUID =
    UUID.fromString(this.parameters["uuid"])
    ?: throw IllegalArgumentException("Failed to fetch follow-up plan sending status: No valid follow-up plan uuid supplied in request")

/*private fun validateEmployeeIdentificationNumber(employeeIdentificationNumber: String?, log: Logger) {
    if (!employeeIdentificationNumber?.matches(Regex("\\d{11}"))!!){
        throw BadRequestException("Invalid employee identification number")
    }
    if ("PROD_GCP".equals(getEnvVar("NAIS_CLUSTER_NAME"), true )) {
        log.trace("Vi er i cluster {}, gjør validering av {}", currentCluster(), verdi)
        require(mod11(W1, verdi) == verdi[9] - '0') { "Første kontrollsiffer $verdi[9] ikke validert" }
        require(mod11(W2, verdi) == verdi[10] - '0') { "Andre kontrollsiffer $verdi[10] ikke validert" }
    }
    else {
        log.trace("Vi er i cluster {}, ingen validering av {}", currentCluster(), verdi)
    }

}*/
private fun validateEmployeeIdentificationNumber(employeeIdentificationNumber: String?, log: Logger) {
    if (!employeeIdentificationNumber?.matches(Regex("\\d{11}"))!!){
        throw BadRequestException("Invalid employee identification number")
    }

    // Check if the first digit is 4 or more to handle D-numbers
    if ("PROD_GCP".equals(getEnvVar("NAIS_CLUSTER_NAME"), true )) {
        val firstDigit = employeeIdentificationNumber.substring(0, 1).toInt()
    val datePart = if (firstDigit >= 4) {
        "0${firstDigit - 4}" + employeeIdentificationNumber.substring(1, 6)
    } else {
        employeeIdentificationNumber.substring(0, 6)
    }

    // Check if the first 6 digits represent a valid date
    val dateFormat = SimpleDateFormat("ddMMyy")
    dateFormat.isLenient = false
    try {
        dateFormat.parse(datePart)
    } catch (e: ParseException) {
        throw BadRequestException("Invalid employee identification number: Date part is not valid")
    }
    }
    else {
        log.trace("Vi er i cluster {}, ingen validering av {}", getEnvVar("NAIS_CLUSTER_NAME", employeeIdentificationNumber))
    }
    // You can add more checks here based on your requirements
}