package no.nav.syfo.client.krrproxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.krrproxy.domain.Kontaktinfo
import no.nav.syfo.client.krrproxy.domain.PostPersonerRequest
import no.nav.syfo.client.krrproxy.domain.PostPersonerResponse
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import org.slf4j.LoggerFactory
import java.util.UUID

class KrrProxyClient(
    private val urlEnv: UrlEnv,
    private val azureAdTokenConsumer: AzureAdClient,
    private val client: HttpClient = httpClientDefault(),
) {
    suspend fun person(fnr: String): Kontaktinfo? {
        val accessToken = "Bearer ${azureAdTokenConsumer.getSystemToken(urlEnv.krrProxyScope)?.accessToken}"
        val response: HttpResponse? =
            try {
                client.post(urlEnv.krrProxyUrl) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append(HttpHeaders.Authorization, accessToken)
                        append(NAV_CALL_ID_HEADER, createCallId())
                    }
                    setBody(PostPersonerRequest.createForFnr(fnr))
                }
            } catch (e: Exception) {
                log.error("Error while calling KRR-PROXY: ${e.message}", e)

                return null
            }

        return when (response?.status) {
            HttpStatusCode.OK -> {
                response.body<PostPersonerResponse>().personer.getOrDefault(fnr, null)
            }

            HttpStatusCode.Unauthorized -> {
                log.error("Could not get kontaktinfo from KRR-PROXY: Unable to authorize")
                null
            }

            else -> {
                log.error(
                    "Call to get  kontaktinfo from KRR-PROXY failed with status: ${response?.status}, " +
                        "response body: ${response?.bodyAsText()}",
                )
                null
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
