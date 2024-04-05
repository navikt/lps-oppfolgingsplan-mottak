package no.nav.syfo.client.isdialogmelding

import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.isdialogmelding.domain.RSOppfoelgingsplan
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.createBearerToken
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory

class IsdialogmeldingClient(
    private val urls: UrlEnv,
    private val azureAdClient: AzureAdClient,
) {
    private val log = LoggerFactory.getLogger(IsdialogmeldingClient::class.qualifiedName)
    private val client = httpClientDefault()

    suspend fun sendLpsPlanToGeneralPractitioner(
        sykmeldtFnr: String,
        planAsPdf: ByteArray,
    ): Boolean {
        val requestUrl = "${urls.isdialogmeldingUrl}/$SEND_LPS_PDF_TO_FASTLEGE_PATH"

        val rsOppfoelgingsplan = RSOppfoelgingsplan(sykmeldtFnr, planAsPdf)
        val token = azureAdClient.getSystemToken(urls.isdialogmeldingClientId)?.accessToken
            ?: throw RuntimeException("Failed to Send plan to fastlege: No token was found")
        val response = try {
            client.post(requestUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, createBearerToken(token))
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
                setBody(rsOppfoelgingsplan)
            }
        } catch (e: Exception) {
            log.error("Exception while sending LPS to fastlege", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                log.info("Successfully sent LPS PDF to fastlege")
                true
            }

            HttpStatusCode.NotFound -> {
                log.warn(
                    "Unable to determine fastlege, or lacking appropiate 'partnerinformasjon'-data",
                )
                false
            }

            else -> {
                log.error("Unable to send LPS plan to fastlege (HTTP error code: ${response.status}")
                false
            }
        }
    }

    companion object {
        private const val SEND_LPS_PDF_TO_FASTLEGE_PATH = "api/v2/send/oppfolgingsplan"
    }
}
