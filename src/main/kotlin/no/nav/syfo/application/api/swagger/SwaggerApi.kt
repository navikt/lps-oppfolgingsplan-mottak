package no.nav.syfo.application.api.swagger

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Routing

fun Routing.registerSwaggerApi() {
    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
}
