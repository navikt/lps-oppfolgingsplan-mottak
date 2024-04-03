package no.nav.syfo.application.environment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.application.exception.MissingRequiredVariableException
import java.io.File

const val LOCAL_PROPERTIES_PATH = "./src/main/resources/localEnv.json"
const val SERVICE_USER_MOUNT_PATH = "/var/run/secrets"
const val DEV_CLUSTER = "dev-gcp"

val objectMapper = ObjectMapper().registerKotlinModule()

@Suppress("LongMethod")
fun getEnv(): ApplicationEnvironment {
    if (isLocal()) {
        return getLocalEnv()
    }
    return ApplicationEnvironment(
        application = ApplicationEnv(
            appName = getEnvVar("NAIS_APP_NAME"),
            port = getEnvVar("APPLICATION_PORT").toInt(),
            cluster = getEnvVar("NAIS_CLUSTER_NAME"),
            coroutineThreadPoolSize = getEnvVar("COROUTINE_THREAD_POOL_SIZE").toInt(),
            electorPath = getEnvVar("ELECTOR_PATH"),
        ),
        auth = AuthEnv(
            maskinporten = AuthMaskinporten(
                wellKnownUrl = getEnvVar("MASKINPORTEN_WELL_KNOWN_URL"),
                issuer = getEnvVar("MASKINPORTEN_ISSUER"),
                scope = getEnvVar("MASKINPORTEN_CUSTOM_SCOPE_NAME"),
                tokenUrl = getEnvVar("MASKINPORTEN_TOKEN_ENDPOINT"),
                clientId = getEnvVar("MASKINPORTEN_CLIENT_ID"),
                clientJwk = getEnvVar("MASKINPORTEN_CLIENT_JWK"),
            ),
            basic = AuthBasic(
                username = getPropertyFromSecretsFile("username"),
                password = getPropertyFromSecretsFile("password"),
            ),
            azuread = AzureAd(
                clientId = getEnvVar("AZURE_APP_CLIENT_ID"),
                clientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
                accessTokenUrl = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                wellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
            ),
        ),
        database = DbEnv(
            dbHost = getEnvVar("GCP_DB_HOST", "127.0.0.1"),
            dbPort = getEnvVar("GCP_DB_PORT", "5432"),
            dbName = getEnvVar("GCP_DB_DATABASE"),
            dbUsername = getEnvVar("GCP_DB_USERNAME"),
            dbPassword = getEnvVar("GCP_DB_PASSWORD"),
        ),
        kafka = KafkaEnv(
            brokerUrl = getEnvVar("KAFKA_BROKERS"),
            schemaRegistry = KafkaSchemaRegistryEnv(
                url = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
                username = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
                password = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
            ),
            sslConfig = KafkaSslEnv(
                truststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
                keystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
                credstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
            ),
        ),
        urls = UrlEnv(
            pdlUrl = getEnvVar("PDL_URL"),
            pdlScope = getEnvVar("PDL_SCOPE"),
            opPdfGenUrl = getEnvVar("OP_PDFGEN_URL"),
            isdialogmeldingUrl = getEnvVar("ISDIALOGMELDING_URL"),
            isdialogmeldingClientId = getEnvVar("ISDIALOGMELDING_CLIENT_ID"),
            dokarkivUrl = getEnvVar("DOKARKIV_URL"),
            dokarkivScope = getEnvVar("DOKARKIV_SCOPE"),
            istilgangskontrollUrl = getEnvVar("ISTILGANGSKONTROLL_URL"),
            istilgangskontrollClientId = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
            krrProxyUrl = getEnvVar(" KRR_PROXY_URL"),
            krrProxyScope = getEnvVar("KRR_PROXY_SCOPE"),
        ),
        altinnLps = AltinnLpsEnv(
            sendToFastlegeRetryThreshold = getEnvVar("SEND_TO_FASTLEGE_RETRY_THRESHOLD").toInt(),
        ),
        toggles = ToggleEnv(
            sendAltinnLpsPlanToNavToggle = getEnvVar("TOGGLE_SEND_ALTINN_LPS_PLAN_TO_NAV").toBoolean(),
            sendAltinnLpsPlanToFastlegeToggle = getEnvVar("TOGGLE_SEND_ALTINN_LPS_PLAN_TO_FASTLEGE").toBoolean(),
            journalforAltinnLpsPlanToggle = getEnvVar("TOGGLE_JOURNALFOR__ALTINN_LPS_PLAN").toBoolean(),
            sendLpsPlanToFastlegeToggle = getEnvVar("TOGGLE_SEND_LPS_PLAN_TO_FASTLEGE").toBoolean(),
            journalforLpsPlanToggle = getEnvVar("TOGGLE_JOURNALFOR_LPS_PLAN").toBoolean(),
        ),
    )
}

fun isLocal(): Boolean = getEnvVar("KTOR_ENV", "local") == "local"

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw MissingRequiredVariableException(varName)

fun getPropertyFromSecretsFile(name: String) =
    File("$SERVICE_USER_MOUNT_PATH/$name").readText()

fun getLocalEnv(): ApplicationEnvironment =
    objectMapper.readValue(File(LOCAL_PROPERTIES_PATH), ApplicationEnvironment::class.java)

fun ApplicationEnvironment.isDev(): Boolean {
    return this.application.cluster == DEV_CLUSTER
}

fun String.toBoolean() = this == "true"
