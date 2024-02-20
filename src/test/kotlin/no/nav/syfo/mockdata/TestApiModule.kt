package no.nav.syfo.mockdata

import io.ktor.server.application.Application
import io.mockk.mockk
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.service.LpsOppfolgingsplanSendingService

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    database: DatabaseInterface,
) {
    val isdialogmeldingConsumer = mockk<IsdialogmeldingClient>(relaxed = true)
    val opPdfGenConsumer = mockk<OpPdfGenClient>(relaxed = true)
    val dokarkivConsumer = mockk<DokarkivClient>(relaxed = true)

    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        httpClient = externalMockEnvironment.mockHttpClient,
        url = externalMockEnvironment.environment.urls.istilgangskontrollUrl,
        clientId = externalMockEnvironment.environment.urls.istilgangskontrollClientId,
    )

    val isdialogmeldingClient = IsdialogmeldingClient(urls = externalMockEnvironment.environment.urls, azureAdClient = externalMockEnvironment.azureAdClient)
    val lpsOppfolgingsplanSendingService = LpsOppfolgingsplanSendingService(
        opPdfGenConsumer = opPdfGenConsumer,
        isdialogmeldingConsumer = isdialogmeldingConsumer,
        dokarkivConsumer = dokarkivConsumer,
        toggles = externalMockEnvironment.environment.toggles,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = database,
        environment = externalMockEnvironment.environment,
        wellKnownMaskinporten = externalMockEnvironment.wellKnownMaskinporten,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        isdialogmeldingClient = isdialogmeldingClient,
        lpsOppfolgingsplanSendingService = lpsOppfolgingsplanSendingService,
    )
}
