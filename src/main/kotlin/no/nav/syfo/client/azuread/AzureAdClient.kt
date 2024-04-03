package no.nav.syfo.client.azuread

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import java.util.concurrent.ConcurrentHashMap
import no.nav.syfo.application.environment.AuthEnv
import no.nav.syfo.client.httpClientProxy
import org.slf4j.LoggerFactory
import kotlin.collections.set

@Suppress("ThrowsCount")
class AzureAdClient(
    authEnv: AuthEnv,
    private val httpClient: HttpClient = httpClientProxy(),
) {
    private val aadAccessTokenUrl = authEnv.azuread.accessTokenUrl
    private val clientId = authEnv.azuread.clientId
    private val clientSecret = authEnv.azuread.clientSecret

    suspend fun getOnBehalfOfToken(scopeClientId: String, token: String): AzureAdToken? = getAccessToken(
        Parameters.build {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            append("assertion", token)
            append("scope", "api://$scopeClientId/.default")
            append("requested_token_use", "on_behalf_of")
        }
    )?.toAzureAdToken()

    suspend fun getSystemToken(scopeClientId: String): AzureAdToken? {
//        val cacheKey = "${CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX}$scopeClientId"
//        log.warn("Cache key: $cacheKey")
//        val cachedToken = cache.get(key = cacheKey)
//        return if (cachedToken?.isExpired() == false) {
//            cachedToken
//        } else {
            val azureAdTokenResponse = getAccessToken(
                Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("grant_type", "client_credentials")
                    append("scope", "api://$scopeClientId/.default")
                }
            )
//            azureAdTokenResponse?.let { token ->
//                token.toAzureAdToken().also {
//                    cache[cacheKey] = it
//                }
//            }
//        }
        return azureAdTokenResponse?.toAzureAdToken()
    }

    private suspend fun getAccessToken(
        formParameters: Parameters,
    ): AzureAdTokenResponse? =
        try {
            val response: HttpResponse = httpClient.post(aadAccessTokenUrl) {
                accept(ContentType.Application.Json)
                setBody(FormDataContent(formParameters))
            }
            response.body<AzureAdTokenResponse>()
        } catch (e: ResponseException) {
            handleUnexpectedResponseException(e)
            null
        }

    private fun handleUnexpectedResponseException(
        responseException: ResponseException,
    ) {
        log.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
    }

    companion object {
        const val CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX = "azuread-token-system-"
        private val cache = ConcurrentHashMap<String, AzureAdToken>()
        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}
