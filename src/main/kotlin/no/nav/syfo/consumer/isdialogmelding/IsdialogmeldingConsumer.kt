package no.nav.syfo.consumer.isdialogmelding

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.consumer.NAV_CALL_ID_HEADER
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.consumer.bearerToken
import no.nav.syfo.consumer.createCallId
import no.nav.syfo.environment.UrlEnv
import org.slf4j.LoggerFactory

@Suppress("TooGenericExceptionCaught")
class IsdialogmeldingConsumer(
    private val urls: UrlEnv,
    private val azureAdTokenConsumer: AzureAdTokenConsumer,
) {
    private val log = LoggerFactory.getLogger(IsdialogmeldingConsumer::class.qualifiedName)
    private val client = httpClient()

    fun sendPlanToGeneralPractitioner(
        sykmeldtFnr: String,
        planAsPdf: ByteArray,
    ): Boolean {
        val requestUrl = "${urls.isdialogmeldingUrl}/$SEND_LPS_PDF_TO_FASTLEGE_PATH"
        val rsOppfoelgingsplan = RSOppfoelgingsplan(sykmeldtFnr, planAsPdf)

        val response = runBlocking {
            val token = azureAdTokenConsumer.getToken(urls.isdialogmeldingClientId)
            try {
                client.post(requestUrl) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append(HttpHeaders.Authorization, bearerToken(token))
                        append(NAV_CALL_ID_HEADER, createCallId())
                    }
                    setBody(rsOppfoelgingsplan)
                }
            } catch (e: Exception) {
                log.error("Exception while sending altinn-LPS to GP", e)
                throw e
            }
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                log.info("Successfully sent PDF to fastlege")
                true
            }
            HttpStatusCode.NotFound -> {
                log.warn("Unable to determine GP, or lacking appropiate" +
                        "'partnerinformasjon'-data")
                false
            }
            else -> {
                log.error("Unable to send altinn-LPS to GP (HTTP error code: ${response.status}")
                false
            }
        }
    }

    companion object {
        private const val SEND_LPS_PDF_TO_FASTLEGE_PATH = "api/v2/send/oppfolgingsplan"
    }
}
