package no.nav.syfo.application.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.auth.installAuthentication
import no.nav.syfo.application.api.swagger.registerSwaggerApi
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.isDev
import no.nav.syfo.application.metric.registerPrometheusApi
import no.nav.syfo.oppfolgingsplanmottak.registerOppfolgingsplanApi
import no.nav.syfo.maskinporten.registerMaskinportenTokenApi

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: ApplicationEnvironment,
) {
    installMetrics()
    installCallId()
    installContentNegotiation()
    installStatusPages()
    installAuthentication(environment)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database
        )
        registerPrometheusApi()
        registerOppfolgingsplanApi(database)
        registerSwaggerApi()
        if (environment.isDev()) {
            registerMaskinportenTokenApi(environment)
        }
    }
}
