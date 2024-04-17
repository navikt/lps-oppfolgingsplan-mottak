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
import no.nav.syfo.client.pdl.domain.Criterion
import no.nav.syfo.client.pdl.domain.Paging
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.PdlIdenterResponse
import no.nav.syfo.client.pdl.domain.PdlPersonResponse
import no.nav.syfo.client.pdl.domain.PdlRequest
import no.nav.syfo.client.pdl.domain.PdlRequestInterface
import no.nav.syfo.client.pdl.domain.PdlSokAdresseResponse
import no.nav.syfo.client.pdl.domain.SearchRule
import no.nav.syfo.client.pdl.domain.SokAdressePdlRequest
import no.nav.syfo.client.pdl.domain.SokAdresseVariables
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
                val responseBody = response.body<PdlIdenterResponse>()
                if (responseBody.errors.isNullOrEmpty()) {
                    val pdlResponse = responseBody.data?.hentIdenter?.identer?.first()?.ident
                    pdlResponse
                } else {
                    log.error("Could not get fnr from PDL: response contains errors: ${responseBody.errors}")
                    null
                }
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
        val response = getPerson(fnr)

        return when (response?.status) {
            HttpStatusCode.OK -> {
                val responseBody = response.body<PdlPersonResponse>()
                if (responseBody.errors.isNullOrEmpty()) {
                    val pdlResponse = responseBody.data
                    pdlResponse
                } else {
                    log.error("Could not get person info from PDL: response contains errors: ${responseBody.errors}")
                    null
                }
            }

            HttpStatusCode.NoContent -> {
                log.error("Could not get person info from PDL: No content found in the response body")
                null
            }

            HttpStatusCode.Unauthorized -> {
                log.error("Could not get person info from PDL: Unable to authorize")
                null
            }

            else -> {
                log.error("Call to  get person info from PDL failed with response: $response")
                null
            }
        }
    }

    private suspend fun getFnr(ident: String): HttpResponse? {
        val graphQuery = this::class.java.getResource(IDENTER_QUERY)?.readText()
            ?: throw FileNotFoundException("Could not found resource: $IDENTER_QUERY")
        val requestBody = PdlRequest(graphQuery, Variables(ident))
        return postCallToPdl(requestBody)
    }

    private suspend fun getPerson(ident: String): HttpResponse? {
        val graphQuery = this::class.java.getResource(PERSON_QUERY)?.readText()
            ?: throw FileNotFoundException("Could not found resource: $PERSON_QUERY")
        val requestBody = PdlRequest(graphQuery, Variables(ident))
        return postCallToPdl(requestBody)
    }

    private suspend fun sokAdresse(postnummer: String): HttpResponse? {
        val graphQuery = this::class.java.getResource(SOK_ADRESSE)?.readText()
            ?: throw FileNotFoundException("Could not found resource: $SOK_ADRESSE")
        val requestBody = SokAdressePdlRequest(
            graphQuery,
            SokAdresseVariables(
                paging = Paging(),
                criteria = listOf(Criterion(searchRule = SearchRule(postnummer)))
            )
        )
        return postCallToPdl(requestBody)
    }

    suspend fun getPoststed(postnummer: String): String? {
        val response = sokAdresse(postnummer)

        return when (response?.status) {
            HttpStatusCode.OK -> {
                val responseBody = response.body<PdlSokAdresseResponse>()
                if (responseBody.errors.isNullOrEmpty()) {
                    val poststed =
                        responseBody.data?.sokAdresse?.hits?.first()?.vegadresse?.poststed
                    log.info("Fetched poststed from PDL: $poststed")
                    poststed
                } else {
                    log.error("Could not get poststed from PDL, response contains errors: ${responseBody.errors}")
                    return null
                }
            }

            HttpStatusCode.NoContent -> {
                log.error("Could not get poststed from PDL: No content found in the response body")
                null
            }

            HttpStatusCode.Unauthorized -> {
                log.error("Could not get poststed from PDL: Unable to authorize")
                null
            }

            else -> {
                log.error("Could not get poststed from PDL: $response")
                null
            }
        }
    }

    private suspend fun postCallToPdl(requestBody: PdlRequestInterface): HttpResponse? {
        val token = azureAdClient.getSystemToken(urls.pdlScope)?.accessToken
        val bearerTokenString = "Bearer $token"

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
