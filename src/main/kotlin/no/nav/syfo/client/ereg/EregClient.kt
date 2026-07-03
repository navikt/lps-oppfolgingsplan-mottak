package no.nav.syfo.client.ereg

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import no.nav.syfo.application.environment.ApplicationEnv
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.domain.EregOrganisasjon
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

    private suspend fun hentOrganisasjon(
        orgnr: String,
        inkluderHierarki: Boolean = false,
    ): HttpResponse {
        val query = if (inkluderHierarki) "?inkluderHierarki=true" else ""
        return try {
            client.get("$eregBaseUrl/ereg/api/v2/organisasjon/$orgnr$query") {
                val token =
                    azureAdClient.getSystemToken(scope)?.accessToken
                        ?: throw RuntimeException("Failed to fetch organization from EREG: No token was found")
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, createBearerToken(token))
                    append(NAV_CONSUMER_ID_HEADER, appEnv.appName)
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
            }
        } catch (e: Exception) {
            log.error("Could not fetch organization from EREG", e)
            throw e
        }
    }

    private suspend fun getOrganisationInformation(orgnr: String): EregOrganisasjonResponse? {
        val response = hentOrganisasjon(orgnr)
        return when (response.status) {
            HttpStatusCode.OK -> response.body<EregOrganisasjonResponse>()
            else -> {
                log.error(
                    "Call to get name by virksomhetsnummer from EREG failed with status:" +
                        " ${response.status}, response body: ${response.bodyAsText()}",
                )
                null
            }
        }
    }

    suspend fun getEmployerOrganisationName(orgnr: String): String? = getOrganisationInformation(orgnr)?.getNavn()

    /**
     * Henter organisasjonen med fullt hierarki (`?inkluderHierarki=true`), slik at underenhetens
     * organisasjonsledd og juridiske enheter er tilgjengelige. Brukes til å avgjøre om et oppgitt
     * orgnummer er en gyldig overordnet enhet for arbeidsstedet.
     *
     * Skiller mellom genuint fravær (404 -> null, korrekt å avvise) og utilgjengelighet (annet enn
     * 200/404 -> kastes, slik at en forbigående ereg-feil blir 500 og kan prøves på nytt — i stedet
     * for et villedende 403 om at arbeidsforholdet ikke finnes).
     */
    suspend fun getOrganisasjonHierarki(orgnr: String): EregOrganisasjon? {
        val response = hentOrganisasjon(orgnr, inkluderHierarki = true)
        return when (response.status) {
            HttpStatusCode.OK -> response.body<EregOrganisasjon>()
            HttpStatusCode.NotFound -> null
            else ->
                throw RuntimeException(
                    "EREG hierarki lookup failed with status: ${response.status}",
                )
        }
    }
}
