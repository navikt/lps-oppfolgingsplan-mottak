package no.nav.syfo.client.pdl

import io.ktor.client.HttpClient
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
import no.nav.syfo.application.exception.PdlBadRequestException
import no.nav.syfo.application.exception.PdlException
import no.nav.syfo.application.exception.PdlGenericException
import no.nav.syfo.application.exception.PdlHttpException
import no.nav.syfo.application.exception.PdlNotFoundException
import no.nav.syfo.application.exception.PdlServerException
import no.nav.syfo.application.exception.PdlUnauthorizedException
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.pdl.domain.Criterion
import no.nav.syfo.client.pdl.domain.Paging
import no.nav.syfo.client.pdl.domain.PdlError
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
    private val client: HttpClient = httpClientDefault(),
) {

    private val log: Logger = LoggerFactory.getLogger(PdlClient::class.qualifiedName)

    suspend fun mostRecentFnr(fnr: String): String? {
        return try {
            val response = getFnr(fnr)
            handleIdenterResponse(response)
        } catch (e: PdlNotFoundException) {
            log.warn("No person found with FNR", e)
            null
        } catch (e: PdlException) {
            log.error("Error getting most recent FNR from PDL", e)
            null
        } catch (e: Exception) {
            log.error("Unexpected error getting most recent FNR from PDL", e)
            null
        }
    }

    suspend fun getPersonInfo(fnr: String): PdlHentPerson? {
        return try {
            val response = getPerson(fnr)
            handlePersonResponse(response)
        } catch (e: PdlNotFoundException) {
            log.warn("Person not found in PDL with FNR", e)
            null
        } catch (e: PdlException) {
            log.error("Error getting person info from PDL", e)
            null
        } catch (e: Exception) {
            log.error("Unexpected error getting person info from PDL", e)
            null
        }
    }

    suspend fun getPoststed(postnummer: String): String? {
        return try {
            val response = sokAdresse(postnummer)
            handleSokAdresseResponse(response, postnummer)
        } catch (e: PdlNotFoundException) {
            log.warn("No poststed found for postnummer: $postnummer", e)
            null
        } catch (e: PdlException) {
            log.error("Error getting poststed from PDL", e)
            null
        } catch (e: Exception) {
            log.error("Unexpected error getting poststed from PDL", e)
            null
        }
    }

    private suspend fun getFnr(ident: String): HttpResponse {
        val graphQuery = this::class.java.getResource(IDENTER_QUERY)?.readText()
            ?: throw FileNotFoundException("Could not found resource: $IDENTER_QUERY")
        val requestBody = PdlRequest(graphQuery, Variables(ident))
        return postCallToPdl(requestBody) ?: throw PdlHttpException("Call to PDL failed", HttpStatusCode.InternalServerError)
    }

    private suspend fun getPerson(ident: String): HttpResponse {
        val graphQuery = this::class.java.getResource(PERSON_QUERY)?.readText()
            ?: throw FileNotFoundException("Could not found resource: $PERSON_QUERY")
        val requestBody = PdlRequest(graphQuery, Variables(ident))
        return postCallToPdl(requestBody) ?: throw PdlHttpException("Call to PDL failed", HttpStatusCode.InternalServerError)
    }

    private suspend fun sokAdresse(postnummer: String): HttpResponse {
        val graphQuery = this::class.java.getResource(SOK_ADRESSE)?.readText()
            ?: throw FileNotFoundException("Could not found resource: $SOK_ADRESSE")
        val requestBody = SokAdressePdlRequest(
            graphQuery,
            SokAdresseVariables(
                paging = Paging(),
                criteria = listOf(Criterion(searchRule = SearchRule(postnummer)))
            )
        )
        return postCallToPdl(requestBody) ?: throw PdlHttpException("Call to PDL failed", HttpStatusCode.InternalServerError)
    }

    private suspend fun postCallToPdl(requestBody: PdlRequestInterface): HttpResponse? {
        val token = azureAdClient.getSystemToken(urls.pdlScope)?.accessToken
            ?: throw PdlUnauthorizedException("Unable to get token for PDL")

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

    private suspend fun handleIdenterResponse(response: HttpResponse): String? {
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseBody = response.body<PdlIdenterResponse>()
                if (responseBody.errors.isNullOrEmpty()) {
                    responseBody.data?.hentIdenter?.identer?.firstOrNull()?.ident
                } else {
                    handlePdlErrors(responseBody.errors, "get FNR from PDL")
                    null // Reached only if error handling doesn't throw
                }
            }
            HttpStatusCode.NoContent -> throw PdlNotFoundException("No content found in the response body")
            HttpStatusCode.Unauthorized -> throw PdlUnauthorizedException("Unable to authorize")
            else -> throw PdlHttpException("Unexpected HTTP status: ${response.status}", response.status)
        }
    }

    private suspend fun handlePersonResponse(response: HttpResponse): PdlHentPerson? {
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseBody = response.body<PdlPersonResponse>()
                if (responseBody.errors.isNullOrEmpty()) {
                    responseBody.data
                } else {
                    handlePdlErrors(responseBody.errors, "get person info from PDL")
                    null // Reached only if error handling doesn't throw
                }
            }
            HttpStatusCode.NoContent -> throw PdlNotFoundException("No content found in the response body")
            HttpStatusCode.Unauthorized -> throw PdlUnauthorizedException("Unable to authorize")
            else -> throw PdlHttpException("Unexpected HTTP status: ${response.status}", response.status)
        }
    }

    private suspend fun handleSokAdresseResponse(response: HttpResponse, postnummer: String): String? {
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseBody = response.body<PdlSokAdresseResponse>()
                if (responseBody.errors.isNullOrEmpty()) {
                    val poststed = responseBody.data?.sokAdresse?.hits?.firstOrNull()?.vegadresse?.poststed
                    if (poststed.isNullOrEmpty()) {
                        log.info("No poststed found for postnummer: $postnummer")
                        null
                    } else {
                        log.info("Fetched poststed from PDL: $poststed")
                        poststed
                    }
                } else {
                    handlePdlErrors(responseBody.errors, "get poststed from PDL")
                    null // Reached only if error handling doesn't throw
                }
            }
            HttpStatusCode.NoContent -> throw PdlNotFoundException("No content found in the response body")
            HttpStatusCode.Unauthorized -> throw PdlUnauthorizedException("Unable to authorize")
            else -> throw PdlHttpException("Unexpected HTTP status: ${response.status}", response.status)
        }
    }

    private fun handlePdlErrors(errors: List<PdlError>, operation: String): Nothing {
        val error = errors.first()
        val errorMessage = error.message

        when (error.extensions.code) {
            "not_found" -> throw PdlNotFoundException("Could not $operation: 'Fant ikke person i PDL': $errorMessage")
            "bad_request" -> throw PdlBadRequestException("Could not $operation: 'Ugyldig ident/Ugyldig spørring/For stor spørring': $errorMessage")
            "server_error" -> throw PdlServerException("Could not $operation: 'Intern feil i PDL Api': $errorMessage")
            "unauthorized" -> throw PdlUnauthorizedException(
                "Could not $operation: 'Gyldig, men feil type token eller ikke tilgang til tjenesten'. " +
                        "Message: $errorMessage; " +
                        "Cause: ${error.extensions.details.cause}",
                error.extensions.details.policy
            )
            else -> throw PdlGenericException("Could not $operation: Message: $errorMessage")
        }
    }

    companion object {
        private const val PDL_BEHANDLINGSNUMMER_HEADER = "behandlingsnummer"
        private const val BEHANDLINGSNUMMER_DIGITAL_OPPFOLGINGSPLAN = "B426"
        private const val IDENTER_QUERY = "/pdl/hentIdenter.graphql"
        private const val PERSON_QUERY = "/pdl/hentPerson.graphql"
        private const val SOK_ADRESSE = "/pdl/sokAdresse.graphql"
    }
}