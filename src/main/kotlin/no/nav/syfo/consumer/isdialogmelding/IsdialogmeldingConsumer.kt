package no.nav.syfo.consumer.isdialogmelding

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.consumer.NAV_CALL_ID_HEADER
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.consumer.createBearerToken
import no.nav.syfo.consumer.createCallId
import no.nav.syfo.environment.UrlEnv
import org.slf4j.LoggerFactory
import java.io.Serializable

class IsdialogmeldingConsumer(
    private val urls: UrlEnv,
    private val azureAdTokenConsumer: AzureAdTokenConsumer,
) {
    private val log = LoggerFactory.getLogger(IsdialogmeldingConsumer::class.qualifiedName)
    private val client = httpClient()

    suspend fun sendAltinnLpsPlanToFastlege(
        sykmeldtFnr: String,
        planAsPdf: ByteArray,
    ): Boolean {
        val requestUrl = "${urls.isdialogmeldingUrl}/$SEND_ALTINN_LPS_PDF_TO_FASTLEGE_PATH"
        val rsOppfoelgingsplan = RSOppfoelgingsplan(sykmeldtFnr, planAsPdf)
        val token = azureAdTokenConsumer.getToken(urls.isdialogmeldingClientId)
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
            log.error("Exception while sending altinn-LPS to fastlege", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                log.info("Successfully sent PDF to fastlege")
                true
            }
            HttpStatusCode.NotFound -> {
                log.warn(
                    "Unable to determine fastlege, or lacking appropiate" +
                        "'partnerinformasjon'-data",
                )
                false
            }
            else -> {
                log.error("Unable to send altinn-LPS to fastlege (HTTP error code: ${response.status}")
                false
            }
        }
    }

    suspend fun sendLpsPlanToFastlege(
        sykmeldtFnr: String,
        planAsPdf: ByteArray,
    ): String? {
        val requestUrl = "${urls.isdialogmeldingUrl}/$SEND_LPS_PDF_TO_FASTLEGE_PATH"
        val rsOppfoelgingsplan = RSOppfoelgingsplan(sykmeldtFnr, planAsPdf)
        val token = azureAdTokenConsumer.getToken(urls.isdialogmeldingClientId)
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
            log.error("Exception while sending altinn-LPS to fastlege", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                log.info("Successfully sent LPS PDF to fastlege")
                response.body<OppfolgingsplanResponse>().bestillingUuid
            }
            HttpStatusCode.NotFound -> {
                log.warn(
                    "Unable to determine fastlege, or lacking appropiate" +
                        "'partnerinformasjon'-data",
                )
                null
            }
            else -> {
                log.error("Unable to send LPS-plan to fastlege (HTTP error code: ${response.status}")
                null
            }
        }
    }

    suspend fun getDeltMedFastlegeStatus(
        bestillingsUuid: String,
    ): DelingMedFastlegeStatusResponse? {
        val requestUrl = "${urls.isdialogmeldingUrl}/$GET_LPS_DELT_MED_FASTLEGE_STATUS_PATH/$bestillingsUuid"
        val token = azureAdTokenConsumer.getToken(urls.isdialogmeldingClientId)
        val response = try {
            client.get(requestUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, createBearerToken(token))
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
            }
        } catch (e: Exception) {
            log.error("Exception while fetching sending to fastlege status", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                log.info("Successfully fetched sending to fastlege status")
                response.body<DelingMedFastlegeStatusResponse>()
            }
            else -> {
                log.error("Unable to fetch sending to fastlege status (HTTP error code: ${response.status}")
                null
            }
        }
    }

    companion object {
        private const val SEND_ALTINN_LPS_PDF_TO_FASTLEGE_PATH = "api/v2/send/oppfolgingsplan"
        private const val SEND_LPS_PDF_TO_FASTLEGE_PATH = "api/v3/send/oppfolgingsplan/"
        private const val GET_LPS_DELT_MED_FASTLEGE_STATUS_PATH = "/api/v2/sent/status/behandler/"
    }
}

data class DelingMedFastlegeStatusResponse(
    val sendingToFastlegeId: String,
    val isSent: Boolean,
) : Serializable

data class OppfolgingsplanResponse(
    val bestillingUuid: String?,
) : Serializable
