package no.nav.syfo.application.api.swagger

import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Routing.registerSwaggerApi() {
    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
}
