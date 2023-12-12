package no.nav.syfo.consumer.oppdfgen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.consumer.NAV_CALL_ID_HEADER
import no.nav.syfo.consumer.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.consumer.createCallId
import no.nav.syfo.environment.ApplicationEnv
import no.nav.syfo.environment.UrlEnv
import no.nav.syfo.service.domain.Fagmelding
import org.slf4j.LoggerFactory
import java.util.*

@Suppress("TooGenericExceptionCaught")
class OpPdfGenConsumer(
    private val urls: UrlEnv,
    private val appEnv: ApplicationEnv
) {
    private val client = httpClient()

    fun generatedPdfResponse(fagmelding: Fagmelding): ByteArray? {
        val requestUrl = "${urls.opPdfGenUrl}/$pathUrl"
        val response = runBlocking {
            val requestBody = mapper.writeValueAsString(fagmelding)
            try {
                client.post(requestUrl) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append(NAV_CONSUMER_ID_HEADER, appEnv.appName)
                        append(NAV_CALL_ID_HEADER, createCallId())
                    }
                    setBody(requestBody)
                }
            } catch (e: Exception) {
                log.error("Call to get generate pdf for LPS-plan failed due to exception: ${e.message}", e)
                throw e
            }
        }

        return when(response.status) {
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
        private val log = LoggerFactory.getLogger(OpPdfGenConsumer::class.qualifiedName)
        private const val pathUrl = "api/v1/genpdf/opservice/oppfolgingsplanlps"

        private val mapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
