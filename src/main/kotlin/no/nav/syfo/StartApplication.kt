package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import no.nav.syfo.api.nais.registerNaisApi
import no.nav.syfo.api.nais.registerPrometheusApi
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = false, var initialized: Boolean = false)
val state: ApplicationState = ApplicationState()
fun main() {
    val server = embeddedServer(
        Netty,
        applicationEngineEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load())

            connector {
                port = getEnvVar("APPLICATION_PORT", "8080").toInt()
            }

            module {
                state.running = true
                serverModule()
            }

        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}

fun Application.serverModule() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    routing {
        registerPrometheusApi()
        registerNaisApi(state)
    }

    state.initialized = true
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
