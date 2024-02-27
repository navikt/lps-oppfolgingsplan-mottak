package no.nav.syfo.oppfolgingsplanmottak

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.auth.JwtIssuerType
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.util.getLpsOrgnumberFromClaims
import no.nav.syfo.util.getOrgnumberFromClaims
import java.util.*

fun Routing.registerOppfolgingsplanApi(
    database: DatabaseInterface,
) {
    route("/api/v1/followupplan/write") {
        authenticate(JwtIssuerType.MASKINPORTEN.name) {
            post {
                val followUpPlanDTO = call.receive<FollowUpPlanDTO>()
                val uuid = UUID.randomUUID()

                database.storeFollowUpPlan(
                    uuid = uuid,
                    followUpPlanDTO = followUpPlanDTO,
                    organizationNumber = getOrgnumberFromClaims(),
                    lpsOrgnumber = getLpsOrgnumberFromClaims()
                )

                call.respond(FollowUpPlanResponse(uuid.toString()))
            }
        }
    }
}
