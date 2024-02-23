package no.nav.syfo.client.isdialogmelding

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.isdialogmelding.domain.RSOppfoelgingsplan
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.createBearerToken
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import java.io.Serializable

class IsdialogmeldingClient(
    private val urls: UrlEnv,
    private val azureAdClient: AzureAdClient,
) {
    private val log = LoggerFactory.getLogger(IsdialogmeldingClient::class.qualifiedName)
    private val client = httpClientDefault()

    suspend fun sendAltinnLpsPlanToFastlege(
        sykmeldtFnr: String,
        planAsPdf: ByteArray,
    ): Boolean {
        val requestUrl = "${urls.isdialogmeldingUrl}/$SEND_ALTINN_LPS_PDF_TO_FASTLEGE_PATH"
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
        log.error("Sending plan to fastlege req2, fnr: $requestUrl")
        log.error("Sending plan to fastlege req1 old, fnr:${urls.isdialogmeldingUrl}/$SEND_ALTINN_LPS_PDF_TO_FASTLEGE_PATH")
        val rsOppfoelgingsplan = RSOppfoelgingsplan(sykmeldtFnr, planAsPdf)
        val token = azureAdClient.getSystemToken(urls.isdialogmeldingClientId)?.accessToken
            ?: throw RuntimeException("Failed to Send plan to fastlege: No token was found")
        log.warn("Sending plan to fastlege, fnr: $sykmeldtFnr")
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
                response.body<RSSendOppfolgingsplan>().bestillingUuid
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
        val token = azureAdClient.getSystemToken(urls.isdialogmeldingClientId)?.accessToken
            ?: throw RuntimeException("Failed to Send plan to fastlege: No token was found")
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

data class RSSendOppfolgingsplan(
    val sykmeldtFnr: String,
    val bestillingUuid: String?,
) : Serializable
