package no.nav.syfo.mockdata

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpResponseData
import no.nav.syfo.client.azuread.AzureAdTokenResponse

fun MockRequestHandleScope.azureAdMockResponse(): HttpResponseData = respond(
    AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type",
    )
)
