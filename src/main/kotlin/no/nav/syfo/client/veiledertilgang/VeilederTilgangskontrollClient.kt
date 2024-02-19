package no.nav.syfo.client.veiledertilgang

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.createBearerToken
import org.slf4j.LoggerFactory

class VeilederTilgangskontrollClient(
    private val azureAdClient: AzureAdClient,
    url: String,
    private val clientId: String,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    private val tilgangskontrollPersonUrl = "${url}$TILGANGSKONTROLL_PERSON_PATH"

    suspend fun hasAccess(callId: String, personIdent: PersonIdent, token: String): Boolean {
        val onBehalfOfToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientId,
            token = token,
        )?.accessToken ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        return try {
            val tilgang = httpClient.get(tilgangskontrollPersonUrl) {
                header(HttpHeaders.Authorization, createBearerToken(onBehalfOfToken))
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_TILGANGSKONTROLL_PERSON_SUCCESS.increment()
            tilgang.body<Tilgang>().erGodkjent
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                COUNT_CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN.increment()
            } else {
                handleUnexpectedResponseException(e.response, callId)
            }
            false
        }
    }

    private fun handleUnexpectedResponseException(
        response: HttpResponse,
        callId: String,
    ) {
        log.error(
            "Error while requesting access to person from istilgangskontroll with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            StructuredArguments.keyValue("callId", callId)
        )
        COUNT_CALL_TILGANGSKONTROLL_PERSON_FAIL.increment()
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeilederTilgangskontrollClient::class.java)

        const val TILGANGSKONTROLL_PERSON_PATH = "/api/tilgang/navident/person"
    }
}
