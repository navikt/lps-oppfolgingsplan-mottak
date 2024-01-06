package no.nav.syfo.consumer.pdl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.environment.UrlEnv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException

class PdlConsumer(
    private val urls: UrlEnv,
    private val azureAdTokenConsumer: AzureAdTokenConsumer,
) {
    private val client = httpClient()
    private val log: Logger = LoggerFactory.getLogger(PdlConsumer::class.qualifiedName)

    suspend fun mostRecentFnr(fnr: String): String? {
        val response = getFnr(fnr)

        return when (response?.status) {
            HttpStatusCode.OK -> {
                val pdlResponse = response.body<PdlIdentResponse>().data?.hentIdenter?.identer?.first()?.ident
                pdlResponse
            }

            HttpStatusCode.NoContent -> {
                log.error("Could not get fnr from PDL: No content found in the response body")
                null
            }

            HttpStatusCode.Unauthorized -> {
                log.error("Could not get fnr from PDL: Unable to authorize")
                null
            }

            else -> {
                log.error("Could not get fnr from PDL: $response")
                null
            }
        }
    }

    private suspend fun getFnr(ident: String): HttpResponse? {
        val token = azureAdTokenConsumer.getToken(urls.pdlScope)
        val bearerTokenString = "Bearer $token"
        val graphQuery = this::class.java.getResource(IDENTER_QUERY)?.readText()?.replace("[\n\r]", "")
                ?: throw FileNotFoundException("Could not found resource: $IDENTER_QUERY")
        val requestBody = PdlRequest(graphQuery, Variables(ident))
        return try {
            client.post(urls.pdlUrl) {
                headers {
                    append(PDL_BEHANDLINGSNUMMER_HEADER, BEHANDLINGSNUMMER_DIGITAL_OPPFOLGINGSPLAN)
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, bearerTokenString)
                }
                setBody(requestBody)
            }
        } catch (e: Exception) {
            log.error("Error while calling PDL: ${e.message}", e)
            null
        }
    }
}
