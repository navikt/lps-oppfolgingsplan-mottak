package no.nav.syfo.client.ereg

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import no.nav.syfo.application.environment.ApplicationEnv
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.domain.EregOrganisasjonResponse
import no.nav.syfo.client.ereg.domain.getNavn
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.createBearerToken
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory

class EregClient(
    urls: UrlEnv,
    private val appEnv: ApplicationEnv,
    private val azureAdClient: AzureAdClient,

    ) {
    private val eregBaseUrl = urls.eregBaseUrl
    private val scope = urls.eregScope
    private val client = httpClientDefault()
    private val log = LoggerFactory.getLogger(EregClient::class.qualifiedName)

    suspend fun getOrganisationInformation(orgnr: String): EregOrganisasjonResponse? {
        val response = try {
            client.get("${eregBaseUrl}/ereg/api/v2/organisasjon/$orgnr") {
                val token = azureAdClient.getSystemToken(scope)?.accessToken
                    ?: throw RuntimeException("Failed to fetch organization name from EREG: No token was found")
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, createBearerToken(token))
                    append(NAV_CONSUMER_ID_HEADER, appEnv.appName)
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
            }
        } catch (e: Exception) {
            log.error("Could not fetch organization name form EREG", e)
            throw e
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                response.body<EregOrganisasjonResponse>()
            }

            else -> {
                log.error("Call to get name by virksomhetsnummer from EREG failed with status: ${response.status}, response body: ${response.bodyAsText()}")
                null
            }
        }
    }

    suspend fun getEmployerOrganisationName(orgnr: String): String? {
        return getOrganisationInformation(orgnr)?.getNavn()
    }
}
