package no.nav.syfo.util

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import no.nav.syfo.domain.PersonIdent

const val JWT_CLAIM_AZP = "azp"
const val JWT_CLAIM_NAVIDENT = "NAVident"

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun ApplicationCall.getPersonIdent(): PersonIdent? =
    this.request.headers[NAV_PERSONIDENT_HEADER]?.let { PersonIdent(it) }

fun ApplicationCall.getConsumerClientId(): String? =
    getBearerHeader()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }

fun ApplicationCall.getBearerHeader(): String? =
    this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")

fun PipelineContext<Unit, ApplicationCall>.getOrgnumberFromClaims(): String {
    val consumer = call.principal<JWTPrincipal>()?.payload?.getClaim("consumer")?.asMap()

    requireNotNull(consumer)

    return maskinportenIdToOrgnumber(consumer["ID"] as String)
}

fun PipelineContext<Unit, ApplicationCall>.getLpsOrgnumberFromClaims(): String {
    val supplier = call.principal<JWTPrincipal>()?.payload?.getClaim("supplier")?.asMap()

    requireNotNull(supplier)

    return maskinportenIdToOrgnumber(supplier["ID"] as String)
}

fun maskinportenIdToOrgnumber(id: String): String {
    return id.split(":")[1].trim()
}

