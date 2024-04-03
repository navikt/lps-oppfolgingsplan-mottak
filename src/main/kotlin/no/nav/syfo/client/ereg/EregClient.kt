package no.nav.syfo.client.ereg

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.environment.ApplicationEnv
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.ereg.domain.EregOrganisasjonResponse
import no.nav.syfo.client.ereg.domain.getNavn
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.sts.StsClient
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.createBearerToken
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory

class EregClient(
    private val urls: UrlEnv,
    private val appEnv: ApplicationEnv,
    private val stsClient: StsClient,
) {
    private val baseUrl = urls.eregBaseUrl
    private val client = httpClientDefault()
    private val log = LoggerFactory.getLogger(EregClient::class.qualifiedName)

    suspend fun getOrganisationInformation(orgnr: String): EregOrganisasjonResponse? {
        val response = try {
            client.get("${baseUrl}/ereg/api/v1/organisasjon/{$orgnr}") {
                val stsToken = stsClient.token()
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, createBearerToken(stsToken))
                    append(NAV_CONSUMER_ID_HEADER, appEnv.appName)
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
            }
        } catch (e: Exception) {
            log.error("Could not send Altinn-LPS to dokarkiv", e)
            throw e
        }
        return when (response.status) {
            HttpStatusCode.OK -> {
                runBlocking {
                    response.body<EregOrganisasjonResponse>()
                }
            }

            else -> {
                log.error("Call to get name by virksomhetsnummer from EREG failed with status: ${response.status}")
                null
            }
        }
    }

    suspend fun getEmployerOrganisationName(orgnr: String): String? {
        return getOrganisationInformation(orgnr)?.getNavn()
    }
}
