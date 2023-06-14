package no.nav.syfo.environment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.getEnvVar

const val localPropertiesPath = "./src/main/resources/localEnv.json"
val objectMapper = ObjectMapper().registerKotlinModule()

fun getEnv(): Environment {
    if (isLocal()) {
        return getLocalEnv()
    }
    return Environment(
        application = ApplicationEnv(
            port = getEnvVar("APPLICATION_PORT").toInt()
        ),
        auth = AuthEnv(
            maskinporten = AuthMaskinporten(
                issuer = getEnvVar("MASKINPORTEN_ISSUER"),
                wellKnownUrl = getEnvVar("MASKINPORTEN_WELL_KNOWN_URL")
            )
        )
    )
}

data class Environment(
    val application: ApplicationEnv,
    val auth: AuthEnv
)



