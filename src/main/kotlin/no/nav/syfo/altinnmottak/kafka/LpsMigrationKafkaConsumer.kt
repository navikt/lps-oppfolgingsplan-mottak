package no.nav.syfo.altinnmottak.kafka

import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.altinnmottak.database.storeMigratedAltinnLpsOppfolgingsplan
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.pollDurationInMillis
import no.nav.syfo.application.metric.COUNT_METRIKK_MIGRERING_VELLYKKET
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val LPS_MIGRATION_TOPIC = "team-esyfo.syfo-migrering-altinn-planer"

class LpsMigrationKafkaConsumer(
    val env: KafkaEnv,
    val database: DatabaseInterface,
) {
    private val log: Logger = LoggerFactory.getLogger(LpsMigrationKafkaConsumer::class.qualifiedName)
    private val kafkaListener: KafkaConsumer<String, AltinnLpsOppfolgingsplan>

    init {
        val kafkaConfig = kafkaLpsMigrationConsumerConfig(env)
        kafkaListener = KafkaConsumer(kafkaConfig)
        kafkaListener.subscribe(listOf(LPS_MIGRATION_TOPIC))
        log.info("Subscribed to $LPS_MIGRATION_TOPIC")
    }

    fun listen(appState: ApplicationState) {
        while (appState.ready) {
            kafkaListener.poll(pollDurationInMillis).forEach { record ->
                try {
                    log.info("$LPS_MIGRATION_TOPIC: Received new record")
                    receiveAndPersistMigratedLps(record)
                    kafkaListener.commitSync()
                    COUNT_METRIKK_MIGRERING_VELLYKKET.increment()
                } catch (e: Exception) {
                    log.error(
                        "$LPS_MIGRATION_TOPIC: Exception for record with plan ${record.value().uuid}: ${e.message}", e
                    )
                }
            }
        }
    }

    private fun receiveAndPersistMigratedLps(record: ConsumerRecord<String, AltinnLpsOppfolgingsplan>) {
        val migratedLpsPlan = record.value()
        val uuid = migratedLpsPlan.uuid
        database.storeMigratedAltinnLpsOppfolgingsplan(migratedLpsPlan)
        log.info("Altinn-LPS-plan with UUID: $uuid successfully migrated from syfooppfolgingsplanservice")
    }
}
