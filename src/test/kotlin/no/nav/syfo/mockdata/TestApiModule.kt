package no.nav.syfo.mockdata

import io.ktor.server.application.Application
import io.mockk.mockk
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.krrproxy.KrrProxyClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    database: DatabaseInterface,
) {
    val isdialogmeldingClient = mockk<IsdialogmeldingClient>(relaxed = true)
    val followupPlanProducer = mockk<FollowUpPlanProducer>(relaxed = true)
    val dokarkivClient = mockk<DokarkivClient>(relaxed = true)

    val opPdfGenClient = OpPdfGenClient(
        externalMockEnvironment.environment.urls,
        externalMockEnvironment.environment.application,
        PdlClient(
            externalMockEnvironment.environment.urls,
            externalMockEnvironment.azureAdClient,
            externalMockEnvironment.mockHttpClient
        ),
        KrrProxyClient(
            externalMockEnvironment.environment.urls,
            externalMockEnvironment.azureAdClient,
            client = externalMockEnvironment.mockHttpClient
        ),
        externalMockEnvironment.mockHttpClient
    )

    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        httpClient = externalMockEnvironment.mockHttpClient,
        url = externalMockEnvironment.environment.urls.istilgangskontrollUrl,
        clientId = externalMockEnvironment.environment.urls.istilgangskontrollClientId,
    )

    val followUpPlanSendingService = FollowUpPlanSendingService(
        isdialogmeldingClient = isdialogmeldingClient,
        followupPlanProducer = followupPlanProducer,
        opPdfGenClient = opPdfGenClient,
        dokarkivClient = dokarkivClient,
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
        pdlClient = PdlClient(
            externalMockEnvironment.environment.urls,
            externalMockEnvironment.azureAdClient,
            externalMockEnvironment.mockHttpClient
        ),
    )
}
