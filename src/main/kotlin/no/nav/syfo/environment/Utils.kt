package no.nav.syfo.environment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File


const val localPropertiesPath = "./src/main/resources/localEnv.json"
const val serviceuserMounthPath = "/var/run/secrets"
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
                wellKnownUrl = getEnvVar("MASKINPORTEN_WELL_KNOWN_URL"),
                scope = getPropertyFromSecretsFile("scope_lps_write")
            )
        )
    )
}

fun isLocal(): Boolean = getEnvVar("KTOR_ENV", "local") == "local"

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getPropertyFromSecretsFile(name: String) =
    File("$serviceuserMounthPath/$name").readText()

fun getLocalEnv() =
    objectMapper.readValue(File(localPropertiesPath), Environment::class.java)
