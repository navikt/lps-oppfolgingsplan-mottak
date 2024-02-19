package no.nav.syfo.mockdata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.client.commonConfig

fun mockHttpClient(environment: ApplicationEnvironment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.encodedPath
            when {
                requestUrl == "/${environment.auth.azuread.accessTokenUrl}" -> azureAdMockResponse()
                requestUrl.startsWith("/${environment.urls.istilgangskontrollUrl}") -> tilgangskontrollResponse(
                    request
                )
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
