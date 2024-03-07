package no.nav.syfo.client.oppdfgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.altinnmottak.domain.Fagmelding
import no.nav.syfo.application.environment.ApplicationEnv
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory

class OpPdfGenClient(
    private val urls: UrlEnv,
    private val appEnv: ApplicationEnv
) {
    private val client = httpClientDefault()

    suspend fun generatedPdfResponse(fagmelding: Fagmelding): ByteArray? {
        val requestUrl = "${urls.opPdfGenUrl}/$pathUrl"
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
            log.error("Call to get generate PDF for LPS-plan failed due to exception: ${e.message}", e)
            throw e
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                response.body<ByteArray>()
            }

            else -> {
                log.error("Could not generate PDF. Call failed with status: ${response.status}")
                null
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OpPdfGenClient::class.qualifiedName)
        private const val pathUrl = "api/v1/genpdf/opservice/oppfolgingsplanlps"

        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
