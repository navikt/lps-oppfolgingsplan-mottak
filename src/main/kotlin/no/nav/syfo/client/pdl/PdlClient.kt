package no.nav.syfo.client.pdl

import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import java.io.FileNotFoundException
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.PdlIdenterResponse
import no.nav.syfo.client.pdl.domain.PdlPersonResponse
import no.nav.syfo.client.pdl.domain.PdlRequest
import no.nav.syfo.client.pdl.domain.Variables
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PdlClient(
    private val urls: UrlEnv,
    private val azureAdClient: AzureAdClient,
) {
    private val client = httpClientDefault()
    private val log: Logger = LoggerFactory.getLogger(PdlClient::class.qualifiedName)

    suspend fun mostRecentFnr(fnr: String): String? {
        val response = getFnr(fnr)

        return when (response?.status) {
            HttpStatusCode.OK -> {
                val pdlResponse = response.body<PdlIdenterResponse>().data?.hentIdenter?.identer?.first()?.ident
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

    suspend fun getPersonInfo(fnr: String): PdlHentPerson? {
        val response = getPerson("26918198953") //todo

        return when (response?.status) {
            HttpStatusCode.OK -> {
                val pdlResponse = response.body<PdlPersonResponse>().data
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
        val token = azureAdClient.getSystemToken(urls.pdlScope)?.accessToken
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

    private suspend fun getPerson(ident: String): HttpResponse? {
        val token = azureAdClient.getSystemToken(urls.pdlScope)?.accessToken
        val bearerTokenString = "Bearer $token"
        val graphQuery = this::class.java.getResource(PERSON_QUERY)?.readText()?.replace("[\n\r]", "")
            ?: throw FileNotFoundException("Could not found resource: $PERSON_QUERY")
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
