package no.nav.syfo.altinnmottak.kafka

import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.commonProperties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun kafkaLpsMigrationConsumerConfig(
    kafkaEnvironment: KafkaEnv,
): Properties {
    return Properties().apply {
        putAll(commonProperties(kafkaEnvironment))

        this[ConsumerConfig.GROUP_ID_CONFIG] = "altinn-planer-migrering-v1"
        this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            KafkaAltinnLpsOppfolgingsplanDeserializer::class.java.canonicalName
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
        this[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = "" + (10 * 1024 * 1024)
    }
}
