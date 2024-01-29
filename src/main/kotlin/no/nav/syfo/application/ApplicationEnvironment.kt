package no.nav.syfo.application

import no.nav.syfo.application.environment.*

data class ApplicationEnvironment(
    val application: ApplicationEnv,
    val auth: AuthEnv,
    val database: DbEnv,
    val kafka: KafkaEnv,
    val urls: UrlEnv,
    val altinnLps: AltinnLpsEnv,
    val toggles: ToggleEnv,
)
