package no.nav.syfo.client.oppdfgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import kotlinx.coroutines.runBlocking
import no.nav.syfo.altinnmottak.domain.Fagmelding
import no.nav.syfo.application.environment.ApplicationEnv
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.krrproxy.KrrProxyClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory

class OpPdfGenClient(
    private val urls: UrlEnv,
    private val appEnv: ApplicationEnv,
    private val pdlClient: PdlClient,
    private val krrProxyClient: KrrProxyClient,
) {
    private val client = httpClientDefault()
    private val pdlUtils = PdlUtils(pdlClient)

    suspend fun generatedPdfResponse(fagmelding: Fagmelding): ByteArray? {
        val requestUrl = "${urls.opPdfGenUrl}/$ALTINN_PLAN_PATH"
        val requestBody = mapper.writeValueAsString(fagmelding)
        val response = try {
            client.post(requestUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(NAV_CONSUMER_ID_HEADER, appEnv.appName)
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
                setBody(requestBody)
            }
        } catch (e: Exception) {
            log.error("Call to get generate PDF for Altinn LPS-plan failed due to exception: ${e.message}", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                response.body<ByteArray>()
            }

            else -> {
                log.error("Could not generate Altinn PDF. Call failed with status: ${response.status}")
                null
            }
        }
    }

    suspend fun getLpsPdf(followUpPlanDTO: FollowUpPlanDTO): ByteArray? {
        val requestUrl = "${urls.opPdfGenUrl}/$FOLLOWUP_PLAN_PATH"
        val fnr = followUpPlanDTO.employeeIdentificationNumber

        val personInfo = pdlClient.getPersonInfo(fnr)
        val employeeName = pdlUtils.getPersonNameString(personInfo, fnr)
        val employeeAdress = pdlUtils.getPersonAdressString(fnr)

        log.info("QWQW: employeeAdress to print: $employeeAdress")

        val personDigitalContactInfo = krrProxyClient.person(fnr)

        val request = followUpPlanDTO.toOppfolgingsplanOpPdfGenRequest(
            employeeName,
            employeePhoneNumber = personDigitalContactInfo?.mobiltelefonnummer,
            employeeEmail = personDigitalContactInfo?.epostadresse,
            employeeAdress
        )
        val requestBody = mapper.writeValueAsString(request)

        val response = try {
            client.post(requestUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(NAV_CONSUMER_ID_HEADER, appEnv.appName)
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
                setBody(requestBody)
            }
        } catch (e: Exception) {
            log.error("Call to get PDF for LPS-plan failed due to exception: ${e.message}", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                runBlocking {
                    response.body<ByteArray>()
                }
            }

            else -> {
                log.error("Could not generate PDF. Call failed with status: ${response.status}")
                null
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OpPdfGenClient::class.qualifiedName)
        private const val ALTINN_PLAN_PATH = "api/v1/genpdf/opservice/oppfolgingsplanlps"
        private const val FOLLOWUP_PLAN_PATH = "api/v1/genpdf/oppfolging/oppfolgingsplanlps"

        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
