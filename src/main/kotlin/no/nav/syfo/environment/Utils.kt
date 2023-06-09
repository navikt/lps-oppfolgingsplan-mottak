package no.nav.syfo.environment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.server.application.*
import no.nav.syfo.exception.MissingRequiredVariableException
import java.io.File


const val LOCAL_PROPERTIES_PATH = "./src/main/resources/localEnv.json"
const val SERVICE_USER_MOUNT_PATH = "/var/run/secrets"
const val DEV_CLUSTER = "dev-gcp"

val objectMapper = ObjectMapper().registerKotlinModule()

fun getEnv(): Environment {
    if (isLocal()) {
        return getLocalEnv()
    }
    return Environment(
        application = ApplicationEnv(
            port = getEnvVar("APPLICATION_PORT").toInt(),
            cluster = getEnvVar("NAIS_CLUSTER_NAME"),
        ),
        auth = AuthEnv(
            maskinporten = AuthMaskinporten(
                wellKnownUrl = getEnvVar("MASKINPORTEN_WELL_KNOWN_URL"),
                issuer = getEnvVar("MASKINPORTEN_ISSUER"),
                scope = getEnvVar("MASKINPORTEN_SCOPES"),
                tokenUrl = getEnvVar("MASKINPORTEN_TOKEN_ENDPOINT"),
                clientId = getEnvVar("MASKINPORTEN_CLIENT_ID"),
                clientJwk = getEnvVar("MASKINPORTEN_CLIENT_JWK"),
            ),
            basic = AuthBasic(
                username = getPropertyFromSecretsFile("username"),
                password = getPropertyFromSecretsFile("password"),
            )
        ),
        database = DbEnv(
            dbHost = getEnvVar("GCP_DB_HOST", "127.0.0.1"),
            dbPort = getEnvVar("GCP_DB_PORT", "5432"),
            dbName = getEnvVar("GCP_DB_DATABASE"),
            dbUsername = getEnvVar("GCP_DB_USERNAME"),
            dbPassword = getEnvVar("GCP_DB_PASSWORD"),
        )
    )
}

fun isLocal(): Boolean = getEnvVar("KTOR_ENV", "local") == "local"

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw MissingRequiredVariableException(varName)

fun getPropertyFromSecretsFile(name: String) =
    File("$SERVICE_USER_MOUNT_PATH/$name").readText()

fun getLocalEnv(): Environment =
    objectMapper.readValue(File(LOCAL_PROPERTIES_PATH), Environment::class.java)

fun Application.isDev(env: Environment, codeToRun: () -> Unit) {
    if (env.application.cluster == DEV_CLUSTER) codeToRun()
}
