package no.nav.syfo.client.azuread

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import no.nav.syfo.client.httpClientProxy
import no.nav.syfo.application.environment.AuthEnv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.ws.rs.BadRequestException
import javax.ws.rs.InternalServerErrorException
import kotlin.collections.set

const val TWO_MINUTES_IN_SECONDS = 120L

@Suppress("ThrowsCount")
class AzureAdClient(authEnv: AuthEnv) {
    private val aadAccessTokenUrl = authEnv.azuread.accessTokenUrl
    private val clientId = authEnv.azuread.clientId
    private val clientSecret = authEnv.azuread.clientSecret
    private val log: Logger = LoggerFactory.getLogger(AzureAdAccessToken::class.qualifiedName)
    private val httpClientWithProxy = httpClientProxy()

    @Volatile
    private var tokenMap = HashMap<String, AzureAdAccessToken>()

    suspend fun getToken(resource: String): String {
        val omToMinutter = Instant.now().plusSeconds(TWO_MINUTES_IN_SECONDS)

        val token: AzureAdAccessToken? = tokenMap[resource]

        if (token == null || token.issuedOn!!.plusSeconds(token.expires_in).isBefore(omToMinutter)) {
            log.info("Henter nytt token fra Azure AD for scope : $resource")

            val response = httpClientWithProxy.post(aadAccessTokenUrl) {
                accept(ContentType.Application.Json)

                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", clientId)
                            append("scope", resource)
                            append("grant_type", "client_credentials")
                            append("client_secret", clientSecret)
                        },
                    ),
                )
            }
            when (response.status) {
                HttpStatusCode.OK ->
                    tokenMap[resource] = response.body()
                HttpStatusCode.BadGateway ->
                    throw BadRequestException(exceptionErrorMessage("Bad request - $resource"))
                HttpStatusCode.InternalServerError ->
                    throw InternalServerErrorException(exceptionErrorMessage("Internal server error - $response"))
                else ->
                    throw RuntimeException(exceptionErrorMessage("Exception - $response"))
            }
        }
        return tokenMap[resource]!!.access_token
    }

    private fun exceptionErrorMessage(msg: String) = "Could not get token from AzureAD: $msg"
}


@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureAdAccessToken(
    val access_token: String,
    val expires_in: Long,
    val issuedOn: Instant? = Instant.now(),
)
