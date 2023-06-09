package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import no.nav.syfo.api.lps.registerOppfolgingsplanApi
import no.nav.syfo.api.nais.registerNaisApi
import no.nav.syfo.api.nais.registerPrometheusApi
import no.nav.syfo.api.setupAuth
import no.nav.syfo.api.test.registerMaskinportenTokenApi
import no.nav.syfo.db.Database
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.grantAccessToIAMUsers
import no.nav.syfo.environment.Environment
import no.nav.syfo.environment.getEnv
import no.nav.syfo.environment.isDev
import java.util.concurrent.TimeUnit

val state: ApplicationState = ApplicationState()
const val SERVER_SHUTDOWN_GRACE_PERIOD = 10L
const val SERVER_SHUTDOWN_TIMEOUT = 10L
lateinit var database: DatabaseInterface

fun main() {
    val env = getEnv()
    val server = embeddedServer(
        Netty,
        applicationEngineEnvironment {
            database = Database(env.database)
            database.grantAccessToIAMUsers()
            connector {
                port = env.application.port
            }

            module {
                state.running = true
                serverModule(env)
            }
        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(SERVER_SHUTDOWN_GRACE_PERIOD, SERVER_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}

fun Application.serverModule(env: Environment) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    setupAuth(env)

    routing {
        registerNaisApi(state)
        registerPrometheusApi()
        registerOppfolgingsplanApi(database)
    }

    isDev(env) {
        routing {
            registerMaskinportenTokenApi(env)
        }
    }

    state.initialized = true
}

data class ApplicationState(var running: Boolean = false, var initialized: Boolean = false)
