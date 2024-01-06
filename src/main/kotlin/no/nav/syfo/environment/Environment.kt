package no.nav.syfo.environment

data class Environment(
    val application: ApplicationEnv,
    val auth: AuthEnv,
    val database: DbEnv,
    val kafka: KafkaEnv,
    val urls: UrlEnv,
    val altinnLps: AltinnLpsEnv,
    val toggles: ToggleEnv,
)
