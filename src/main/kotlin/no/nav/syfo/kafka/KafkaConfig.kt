package no.nav.syfo.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.syfo.environment.KafkaEnv
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.util.*

const val ALTINNKANAL_TOPIC = "alf.aapen-altinn-oppfolgingsplan-mottatt-v2"
const val OPPFOLGINGSPLAN_LPS_NAV_TOPIC = "team-esyfo.aapen-syfo-oppfolgingsplan-lps-nav-v2"

const val JAVA_KEYSTORE = "JKS"
const val PKCS12 = "PKCS12"
const val SSL = "SSL"
const val USER_INFO = "USER_INFO"
const val ONE_SECOND_IN_MILLIS = 1000L

val pollDurationInMillis = Duration.ofMillis(ONE_SECOND_IN_MILLIS)

fun commonProperties(env: KafkaEnv): Properties {
    val sslConfig = env.sslConfig

    return HashMap<String, String>().apply {
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SSL)
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "") // Disable server host name verification
        put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, JAVA_KEYSTORE)
        put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, PKCS12)
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslConfig.truststoreLocation)
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslConfig.credstorePassword)
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslConfig.keystoreLocation)
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslConfig.credstorePassword)
        put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslConfig.credstorePassword)
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.brokerUrl)
        remove(SaslConfigs.SASL_MECHANISM)
        remove(SaslConfigs.SASL_JAAS_CONFIG)
        remove(SaslConfigs.SASL_MECHANISM)
    }.toProperties()
}

fun consumerProperties(env: KafkaEnv): Properties {
    val userinfoConfig = "${env.schemaRegistry.username}:${env.schemaRegistry.password}"
    val consumerProperties = commonProperties(env)

    return consumerProperties.apply {
        put(CommonClientConfigs.GROUP_ID_CONFIG, "lps-oppfolgingsplan-mottak")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

        put(BASIC_AUTH_CREDENTIALS_SOURCE, USER_INFO)
        put(SchemaRegistryClientConfig.USER_INFO_CONFIG, userinfoConfig)
        put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistry.url)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer")
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer")
        put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true")
    }
}

fun producerProperties(env: KafkaEnv): Properties {
    val producerProperties = commonProperties(env)

    return producerProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonKafkaSerializer::class.java)
    }

}
