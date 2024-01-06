package no.nav.syfo.environment

data class KafkaEnv(
    val brokerUrl: String,
    val schemaRegistry: KafkaSchemaRegistryEnv,
    val sslConfig: KafkaSslEnv,
)

data class KafkaSslEnv(
    val truststoreLocation: String,
    val keystoreLocation: String,
    val credstorePassword: String,
)

data class KafkaSchemaRegistryEnv(
    val url: String,
    val username: String,
    val password: String,
)
