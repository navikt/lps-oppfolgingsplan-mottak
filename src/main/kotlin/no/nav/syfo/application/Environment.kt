package no.nav.syfo.application

import no.nav.syfo.application.environment.AltinnLpsEnv
import no.nav.syfo.application.environment.ApplicationEnv
import no.nav.syfo.application.environment.AuthEnv
import no.nav.syfo.application.environment.DbEnv
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.application.environment.UrlEnv

data class Environment(
    val application: ApplicationEnv,
    val auth: AuthEnv,
    val database: DbEnv,
    val kafka: KafkaEnv,
    val urls: UrlEnv,
    val altinnLps: AltinnLpsEnv,
    val toggles: ToggleEnv,
)
