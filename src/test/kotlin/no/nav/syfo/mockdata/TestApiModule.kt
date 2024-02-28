package no.nav.syfo.mockdata

import io.ktor.server.application.Application
import io.mockk.mockk
import no.nav.syfo.altinnmottak.FollowUpPlanSendingService
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    database: DatabaseInterface,
) {
    val isdialogmeldingConsumer = mockk<IsdialogmeldingClient>(relaxed = true)

    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        httpClient = externalMockEnvironment.mockHttpClient,
        url = externalMockEnvironment.environment.urls.istilgangskontrollUrl,
        clientId = externalMockEnvironment.environment.urls.istilgangskontrollClientId,
    )

    val followUpPlanSendingService = FollowUpPlanSendingService(
        isdialogmeldingConsumer = isdialogmeldingConsumer,
        toggles = externalMockEnvironment.environment.toggles,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = database,
        environment = externalMockEnvironment.environment,
        wellKnownMaskinporten = externalMockEnvironment.wellKnownMaskinporten,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        followUpPlanSendingService = followUpPlanSendingService,
    )
}
