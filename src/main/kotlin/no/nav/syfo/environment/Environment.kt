package no.nav.syfo.environment

data class Environment(
    val application: ApplicationEnv,
    val auth: AuthEnv
)
