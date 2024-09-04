package no.nav.syfo.application.api

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.auth.AzureAdJwtIssuer
import no.nav.syfo.application.api.auth.MaskinportenJwtIssuer
import no.nav.syfo.application.api.auth.configureAzureAdJwt
import no.nav.syfo.application.api.auth.configureBasicAuthentication
import no.nav.syfo.application.api.auth.configureMaskinportenJwt
import no.nav.syfo.application.api.swagger.registerSwaggerApi
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.isDev
import no.nav.syfo.application.metric.registerPrometheusApi
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.client.wellknown.WellKnown
import no.nav.syfo.maskinporten.registerMaskinportenTokenApi
import no.nav.syfo.oppfolgingsplanmottak.registerFollowUpPlanApi
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.oppfolgingsplanmottak.validation.FollowUpPlanValidator
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import no.nav.syfo.veileder.registerVeilederApi

@Suppress("LongParameterList")
fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: ApplicationEnvironment,
    wellKnownMaskinporten: WellKnown,
    wellKnownInternalAzureAD: WellKnown,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    followUpPlanSendingService: FollowUpPlanSendingService,
    pdlClient: PdlClient,
    sykmeldingService: SendtSykmeldingService
) {
    installMetrics()
    installCallId()
    installContentNegotiation()
    installStatusPages()

    install(Authentication) {
        configureMaskinportenJwt(
            MaskinportenJwtIssuer(
                validScope = environment.auth.maskinporten.scope,
                wellKnown = wellKnownMaskinporten,
            ),
        )
        configureAzureAdJwt(
            AzureAdJwtIssuer(
                acceptedAudienceList = listOf(environment.auth.azuread.clientId),
                wellKnown = wellKnownInternalAzureAD,
            ),
        )
        if (environment.isDev()) {
            configureBasicAuthentication(environment.auth.basic)
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()
        registerFollowUpPlanApi(
            database,
            followUpPlanSendingService,
            FollowUpPlanValidator(pdlClient, sykmeldingService),
            environment
        )
        registerVeilederApi(
            veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            database = database,
        )
        registerSwaggerApi()
        if (environment.isDev()) {
            registerMaskinportenTokenApi(environment)
        }
    }
}
