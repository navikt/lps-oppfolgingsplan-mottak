package no.nav.syfo.util

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.syfo.application.environment.AuthEnv
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.krrproxy.domain.PostPersonerRequest

class MockServers(
    val urlEnv: UrlEnv,
    val authEnv: AuthEnv,
) {
    fun mockKrrServer(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
        mockServer(urlEnv.krrProxyUrl) {
            val jsonMapper = jacksonObjectMapper()
            post("/rest/v1/personer") {
                val requestBody = jsonMapper.readValue(call.receiveText(), PostPersonerRequest::class.java)
                if (requestBody.personidenter.contains("serverdown")) {
                    call.response.status(HttpStatusCode(500, "Server error"))
                } else {
                    call.respondText(
                        jsonMapper.writeValueAsString(dkifPostPersonerResponse),
                        ContentType.Application.Json,
                        HttpStatusCode.OK,
                    )
                }
            }
        }

    fun mockServer(
        url: String,
        route: Route.() -> Unit,
    ): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
        embeddedServer(
            factory = Netty,
            port = url.extractPortFromUrl(),
        ) {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            routing {
                route(this)
            }
        }
}
