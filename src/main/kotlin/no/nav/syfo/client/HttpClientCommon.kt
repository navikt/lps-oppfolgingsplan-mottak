package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.engine.apache5.Apache5EngineConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.syfo.util.configure
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner
import java.net.ProxySelector

const val REQUEST_RETRY_DELAY = 500L

val commonConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {
    install(ContentNegotiation) {
        jackson { configure() }
    }
    install(HttpRequestRetry) {
        retryOnExceptionIf(2) { _, cause ->
            cause !is ClientRequestException
        }
        constantDelay(REQUEST_RETRY_DELAY)
    }
}

val proxyConfig: HttpClientConfig<Apache5EngineConfig>.() -> Unit = {
    this.commonConfig()
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

fun httpClientDefault() = HttpClient(Apache5, commonConfig)

fun httpClientProxy() = HttpClient(Apache5, proxyConfig)
