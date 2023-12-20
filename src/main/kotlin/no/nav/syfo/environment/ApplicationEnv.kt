package no.nav.syfo.environment

data class ApplicationEnv (
    val appName: String,
    val port: Int,
    val cluster: String,
    val coroutineThreadPoolSize: Int,
)
