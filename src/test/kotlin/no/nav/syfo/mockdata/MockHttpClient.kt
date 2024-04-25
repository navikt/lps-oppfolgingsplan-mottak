package no.nav.syfo.mockdata

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.client.commonConfig
import no.nav.syfo.client.oppdfgen.OpPdfGenClient

fun mockHttpClient(environment: ApplicationEnvironment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.toString()
            when {
                requestUrl.contains("/${environment.auth.azuread.accessTokenUrl}") -> azureAdMockResponse()
                requestUrl.contains("/${environment.urls.istilgangskontrollUrl}") -> tilgangskontrollResponse(
                    request
                )

                requestUrl.startsWith(environment.urls.pdlUrl) -> pdlPersonResponse()
                requestUrl.contains(OpPdfGenClient.FOLLOWUP_PLAN_PATH) -> opPdfGenResponse()
                requestUrl.contains(environment.urls.krrProxyUrl) -> krrResponse()
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
