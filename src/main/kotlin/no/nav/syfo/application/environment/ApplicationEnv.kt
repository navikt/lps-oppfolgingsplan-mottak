package no.nav.syfo.application.environment

data class ApplicationEnv(
    val appName: String,
    val port: Int,
    val cluster: String,
    val coroutineThreadPoolSize: Int,
    val electorPath: String,
)
