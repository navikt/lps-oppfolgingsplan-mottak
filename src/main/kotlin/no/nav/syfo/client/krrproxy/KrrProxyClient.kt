package no.nav.syfo.client.krrproxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import java.util.*
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.krrproxy.domain.Kontaktinfo
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.configuredJacksonMapper
import org.slf4j.LoggerFactory

class KrrProxyClient(
    private val urlEnv: UrlEnv,
    private val azureAdTokenConsumer: AzureAdClient,
    private val client: HttpClient = httpClientDefault(),
) {
    private val objectMapper = configuredJacksonMapper()

    suspend fun person(fnr: String): Kontaktinfo? {
        val accessToken = "Bearer ${azureAdTokenConsumer.getSystemToken(urlEnv.krrProxyScope)?.accessToken}"
        val response: HttpResponse? = try {
            client.get(urlEnv.krrProxyUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, accessToken)
                    append(NAV_PERSONIDENT_HEADER, fnr)
                    append(NAV_CALL_ID_HEADER, createCallId())
                }
            }
        } catch (e: Exception) {
            log.error("Error while calling KRR-PROXY: ${e.message}, stacktrace ${e.printStackTrace()}", e)

            return null
        }



        when (response?.status) {
            HttpStatusCode.OK -> {
                val rawJson: String = response.body()
                return objectMapper.readValue(rawJson, Kontaktinfo::class.java)
            }

            HttpStatusCode.Unauthorized -> {
                log.error("Could not get kontaktinfo from KRR-PROXY: Unable to authorize")
                return null
            }

            else -> {
                log.error(
                    "Call to get  kontaktinfo from KRR-PROXY failed with status: ${response?.status}, WWW-Authenticate header:${
                        response?.headers?.get(
                            "WWW-Authenticate"
                        )
                    }, response body: ${response?.bodyAsText()}"
                )
                return null
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KrrProxyClient::class.java)

        private fun createCallId(): String {
            val randomUUID = UUID.randomUUID().toString()
            return "esyfovarsel-$randomUUID"
        }
    }
}
