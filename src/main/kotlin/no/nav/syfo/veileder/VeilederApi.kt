package no.nav.syfo.veileder

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.exception.ForbiddenAccessVeilederException
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getPersonIdent
import no.nav.syfo.veileder.database.getOppfolgingsplanPdf
import no.nav.syfo.veileder.database.getOppfolgingsplanerMetadataForVeileder
import java.util.UUID

const val VEILEDER_LPS_BASE_PATH = "/api/internad/v1/oppfolgingsplan/lps"
const val VEILEDER_LPS_UUID_PARAM = "uuid"
private const val API_ACTION = "access lps plan for person"

fun Routing.registerVeilederApi(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    database: DatabaseInterface,
) {
    route(VEILEDER_LPS_BASE_PATH) {
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            get {
                val callId = call.getCallId()
                val personIdent = call.personIdent()
                val token = call.bearerHeader()

                val hasAccess =
                    veilederTilgangskontrollClient.hasAccess(
                        callId = callId,
                        personIdent = personIdent,
                        token = token,
                    )

                if (!hasAccess) {
                    throw ForbiddenAccessVeilederException(
                        action = API_ACTION,
                    )
                }

                val plansSharedWithNAV =
                    database
                        .getOppfolgingsplanerMetadataForVeileder(personIdent)
                call.respond(plansSharedWithNAV)
            }

            get("/{$VEILEDER_LPS_UUID_PARAM}") {
                val planUUID = call.uuid()
                val plan = database.getOppfolgingsplanPdf(planUUID)

                requireNotNull(plan) { "Failed to $API_ACTION: No valid $VEILEDER_LPS_UUID_PARAM supplied in request" }

                val callId = call.getCallId()
                val token = call.bearerHeader()

                if (!veilederTilgangskontrollClient.hasAccess(callId, plan.first, token)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respondBytes(plan.second, ContentType.Application.Pdf)
                }
            }
        }
    }
}

private fun ApplicationCall.uuid(): UUID =
    UUID.fromString(this.parameters[VEILEDER_LPS_UUID_PARAM])
        ?: throw IllegalArgumentException("Failed to $API_ACTION: No valid $VEILEDER_LPS_UUID_PARAM supplied in request ")

private fun ApplicationCall.personIdent(): PersonIdent =
    this.getPersonIdent()
        ?: throw IllegalArgumentException("Failed to $API_ACTION: No $NAV_PERSONIDENT_HEADER supplied in request header")

private fun ApplicationCall.bearerHeader(): String =
    this.getBearerHeader()
        ?: throw IllegalArgumentException(
            "Failed to complete the following action: $API_ACTION. No Authorization header supplied",
        )
