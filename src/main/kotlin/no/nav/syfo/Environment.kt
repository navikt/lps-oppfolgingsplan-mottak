package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

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

fun isLocal(): Boolean = getEnvVar("KTOR_ENV", "local") == "local"

private fun getLocalEnv() =
    objectMapper.readValue(File(localPropertiesPath), Environment::class.java)

data class Environment(
    val application: ApplicationEnv,
    val auth: AuthEnv
)

data class ApplicationEnv (
    val port: Int
)

data class AuthEnv(
    val maskinporten: AuthMaskinporten
)
data class AuthMaskinporten(
    val issuer: String,
    val wellKnownUrl: String
)
