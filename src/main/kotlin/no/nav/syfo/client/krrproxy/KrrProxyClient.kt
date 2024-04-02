package no.nav.syfo.client.krrproxy

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.util.*
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.krrproxy.domain.Kontaktinfo
import no.nav.syfo.client.krrproxy.domain.KontaktinfoMapper
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.slf4j.LoggerFactory

class KrrProxyClient(
    private val urlEnv: UrlEnv,
    private val azureAdTokenConsumer: AzureAdClient) {
    private val client = httpClientDefault()

    suspend fun person(fnr: String): Kontaktinfo? {
        val accessToken = "Bearer ${azureAdTokenConsumer.getSystemToken(urlEnv.krrProxyScope)}"
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
            log.error("Error while calling KRR-PROXY: ${e.message}", e)
            return null
        }
        when (response?.status) {
            HttpStatusCode.OK -> {
                val rawJson: String = response.body()
                return KontaktinfoMapper.mapPerson(rawJson)
            }

            HttpStatusCode.Unauthorized -> {
                log.error("Could not get kontaktinfo from KRR-PROXY: Unable to authorize")
                return null
            }

            else -> {
                log.error("Could not get kontaktinfo from KRR-PROXY: $response")
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
