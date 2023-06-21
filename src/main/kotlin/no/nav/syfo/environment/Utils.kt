package no.nav.syfo.environment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.server.application.*
import java.io.File


const val localPropertiesPath = "./src/main/resources/localEnv.json"
const val serviceuserMounthPath = "/var/run/secrets"
const val devCluster = "dev-gcp"

val objectMapper = ObjectMapper().registerKotlinModule()

fun getEnv(): Environment {
    if (isLocal()) {
        return getLocalEnv()
    }
    return Environment(
        application = ApplicationEnv(
            port = getEnvVar("APPLICATION_PORT").toInt(),
            cluster = getEnvVar("NAIS_CLUSTER_NAME")
        ),
        auth = AuthEnv(
            maskinporten = AuthMaskinporten(
                wellKnownUrl = getEnvVar("MASKINPORTEN_WELL_KNOWN_URL"),
                issuer = getEnvVar("MASKINPORTEN_ISSUER"),
                scope = getEnvVar("MASKINPORTEN_SCOPES"),
                tokenUrl = getEnvVar("MASKINPORTEN_TOKEN_ENDPOINT"),
                clientId = getEnvVar("MASKINPORTEN_CLIENT_ID"),
                clientJwk = getEnvVar("MASKINPORTEN_CLIENT_JWK")
            ),
            basic = AuthBasic(
                username = getPropertyFromSecretsFile("username"),
                password = getPropertyFromSecretsFile("password")
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

fun Application.isDev(env: Environment, codeToRun: () -> Unit) {
    if (env.application.cluster == devCluster) codeToRun()
}
