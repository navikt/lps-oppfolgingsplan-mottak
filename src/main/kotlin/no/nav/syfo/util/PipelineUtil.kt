package no.nav.syfo.util

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import no.nav.syfo.application.api.auth.Consumer
import no.nav.syfo.application.api.auth.getOrgnumber
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer

const val JWT_CLAIM_AZP = "azp"
const val JWT_CLAIM_NAVIDENT = "NAVident"

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun ApplicationCall.getPersonIdent(): PersonIdent? =
    this.request.headers[NAV_PERSONIDENT_HEADER]?.let { PersonIdent(it) }

fun ApplicationCall.getConsumerClientId(): String? =
    getBearerHeader()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }

fun ApplicationCall.getNAVIdent(): String {
    val token = getBearerHeader() ?: throw Error("No Authorization header supplied")
    return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}

fun ApplicationCall.getBearerHeader(): String? =
    this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")

fun PipelineContext<Unit, ApplicationCall>.getOrgnumberFromClaims(): Virksomhetsnummer {
    val consumerClaim =
        call.authentication.principal<JWTPrincipal>()?.payload?.getClaim("consumer")?.asString()
    val consumer = consumerClaim?.let { Json.decodeFromString<Consumer>(it) }
    val orgnumber = consumer?.getOrgnumber()

    require(
        orgnumber != null
    )

    return Virksomhetsnummer(orgnumber)
}

fun PipelineContext<Unit, ApplicationCall>.getScopeFromClaims(): String {
    val scope = call.authentication.principal<JWTPrincipal>()?.payload?.getClaim("scope")?.asString()

    require(
        scope != null
    )

    return scope
}
