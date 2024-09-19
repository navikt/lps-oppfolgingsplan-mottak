package no.nav.syfo.client.aareg

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.aareg.domain.AaregArbeidsforholdOversikt
import no.nav.syfo.client.aareg.domain.FinnArbeidsforholdoversikterPrArbeidstakerAPIRequest
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.util.createBearerToken
import org.slf4j.LoggerFactory

class ArbeidsforholdOversiktClient(
    private val azureAdClient: AzureAdClient,
    private val urlEnv: UrlEnv
) {

    private val arbeidsforholdOversiktPath = "${urlEnv.aaregBaseUrl}$ARBEIDSFORHOLD_OVERSIKT_PATH"

    private val httpClient = httpClientDefault()

    suspend fun getArbeidsforhold(personIdent: String): AaregArbeidsforholdOversikt? =
        try {
            val token = azureAdClient.getSystemToken(urlEnv.aaregScope)?.accessToken
                ?: throw RuntimeException("Failed to get Arbeidsforhold: No token was found")

            httpClient.post(arbeidsforholdOversiktPath) {
                header(HttpHeaders.Authorization, createBearerToken(token))
                contentType(ContentType.Application.Json)
                setBody(
                    FinnArbeidsforholdoversikterPrArbeidstakerAPIRequest(
                        arbeidstakerId = personIdent
                    )
                )
            }.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                logger.error("Fant ikke arbeidsforhold for bruker", e)
                null
            } else {
                logger.error("Noe gikk galt ved henting av arbeidsforhold", e)
                throw e
            }
        } catch (e: ServerResponseException) {
            logger.error("Noe gikk galt ved henting av arbeidsforhold", e)
            throw e
        }

    companion object {
        const val ARBEIDSFORHOLD_OVERSIKT_PATH = "/api/v2/arbeidstaker/arbeidsforholdoversikt"
        private val logger = LoggerFactory.getLogger(ArbeidsforholdOversiktClient::class.java)
    }
}
