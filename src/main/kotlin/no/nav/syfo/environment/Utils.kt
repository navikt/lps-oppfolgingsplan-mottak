package no.nav.syfo.environment

import no.nav.syfo.getEnvVar
import java.io.File

fun isLocal(): Boolean = getEnvVar("KTOR_ENV", "local") == "local"

fun getLocalEnv() =
    objectMapper.readValue(File(localPropertiesPath), Environment::class.java)
