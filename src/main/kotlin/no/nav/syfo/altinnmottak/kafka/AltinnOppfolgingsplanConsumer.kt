package no.nav.syfo.altinnmottak.kafka

import no.nav.altinnkanal.avro.ReceivedMessage
import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.consumerProperties
import no.nav.syfo.application.kafka.pollDurationInMillis
import no.nav.syfo.application.metric.COUNT_METRIKK_PROSSESERING_VELLYKKET
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

const val ALTINNKANAL_TOPIC = "alf.aapen-altinn-oppfolgingsplan-mottatt-v2"

class AltinnOppfolgingsplanConsumer(
    val env: KafkaEnv,
    private val altinnLPSService: AltinnLpsService
) {
    private val log: Logger = LoggerFactory.getLogger(AltinnOppfolgingsplanConsumer::class.qualifiedName)
    private val kafkaListener: KafkaConsumer<String, ReceivedMessage>

    init {
        val kafkaConfig = consumerProperties(env)
        kafkaListener = KafkaConsumer(kafkaConfig)
        kafkaListener.subscribe(listOf(ALTINNKANAL_TOPIC))
    }

    suspend fun listen(appState: ApplicationState) {
        while (appState.ready) {
            kafkaListener.poll(pollDurationInMillis).forEach { record ->
                try {
                    val storedLpsUuid = receiveAndPersistLpsFromAltinn(record)
                    kafkaListener.commitSync()
                    altinnLPSService.processLpsPlan(storedLpsUuid)
                    COUNT_METRIKK_PROSSESERING_VELLYKKET.increment()
                } catch (e: Exception) {
                    log.error("Error encountered while processing LPS-plan from altinn-kanal-2: ${e.message}", e)
                }
            }
        }
    }

    private fun receiveAndPersistLpsFromAltinn(record: ConsumerRecord<String, ReceivedMessage>): UUID {
        val receivedMessage = record.value()
        val archiveReference = receivedMessage.getArchiveReference()
        log.info("Receiving Altinn-LPS-plan with archive reference: $archiveReference")
        val payload = receivedMessage.getXmlMessage()
        return altinnLPSService.persistLpsPlan(archiveReference, payload)
    }
}
