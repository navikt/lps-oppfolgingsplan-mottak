package no.nav.syfo.mockdata

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.syfo.util.configuredJacksonMapper

val mapper = configuredJacksonMapper()

fun <T> MockRequestHandleScope.respond(body: T): HttpResponseData =
    respond(
        mapper.writeValueAsString(body),
        HttpStatusCode.OK,
        headersOf(HttpHeaders.ContentType, "application/json")
    )
