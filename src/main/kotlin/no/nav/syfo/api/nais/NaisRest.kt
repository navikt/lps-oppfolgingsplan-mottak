package no.nav.syfo.api.nais

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.ApplicationState

val METRICS_REGISTRY = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)

fun Routing.registerNaisApi(
    applicationState: ApplicationState
) {
    get("/isAlive") {
        if (applicationState.running) {
            call.respondText("Application is alive")
        } else {
            call.respondText("Application is dead", status = HttpStatusCode.InternalServerError)
        }
    }

    get("/isReady") {
        if (applicationState.initialized) {
            call.respondText("Application is ready")
        } else {
            call.respondText("Application is not ready", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Routing.registerPrometheusApi() {
    DefaultExports.initialize()

    get("/prometheus") {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
