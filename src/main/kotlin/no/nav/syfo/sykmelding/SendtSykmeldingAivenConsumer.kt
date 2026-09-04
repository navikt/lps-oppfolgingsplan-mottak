package no.nav.syfo.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.KafkaListener
import no.nav.syfo.application.kafka.consumerProperties
import no.nav.syfo.application.kafka.pollDurationInMillis
import no.nav.syfo.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

const val SENDT_SYKMELDING_TOPIC = "teamsykmelding.syfo-sendt-sykmelding"
private val KAFKA_CLOSE_TIMEOUT: Duration = Duration.ofSeconds(1)

class SendtSykmeldingAivenConsumer internal constructor(
    private val kafkaListener: Consumer<String, String>,
    private val sykmeldingService: SendtSykmeldingService,
) : KafkaListener {
    private val log = LoggerFactory.getLogger(SendtSykmeldingAivenConsumer::class.qualifiedName)
    private val objectMapper = configuredJacksonMapper()

    constructor(env: KafkaEnv, sykmeldingService: SendtSykmeldingService) :
        this(createKafkaListener(env), sykmeldingService)

    override suspend fun listen(applicationState: ApplicationState) {
        try {
            while (applicationState.ready && currentCoroutineContext().isActive) {
                val records = poll()
                currentCoroutineContext().ensureActive()
                records.forEach { record ->
                    currentCoroutineContext().ensureActive()
                    log.info("Received record with key: ${record.key()}")
                    processRecord(record)
                }
            }
        } finally {
            kafkaListener.close(CloseOptions.timeout(KAFKA_CLOSE_TIMEOUT))
        }
    }

    private fun poll(): ConsumerRecords<String, String> = kafkaListener.poll(pollDurationInMillis)

    private fun processRecord(record: ConsumerRecord<String, String?>) {
        try {
            val sykmeldingKafkaMessage: SykmeldingKafkaMessage? =
                record.value()?.let { objectMapper.readValue(it) }
            val sykmeldingId = record.key()

            if (sykmeldingKafkaMessage == null) {
                log.info("Received tombstone record for sykmeldingId: $sykmeldingId ..deleting")
                sykmeldingService.deleteSykmeldingsperioder(sykmeldingId)
            } else {
                log.info("Storing sykmeldingsperioder for sykmeldingId: $sykmeldingId")
                sykmeldingService.persistSykmeldingsperioder(
                    sykmeldingId = sykmeldingId,
                    employeeIdentificationNumber = sykmeldingKafkaMessage.kafkaMetadata.fnr,
                    orgnumber = sykmeldingKafkaMessage.event.arbeidsgiver.orgnummer,
                    sykmeldingsperioder = sykmeldingKafkaMessage.sykmelding.sykmeldingsperioder,
                )
            }
            log.info("Committing offset")
            kafkaListener.commitSync()
        } catch (e: Exception) {
            log.error("Error encountered while processing sykmelding: ${e.message}", e)
        }
    }

    companion object {
        private fun createKafkaListener(env: KafkaEnv): Consumer<String, String> {
            val kafkaConfig =
                consumerProperties(env).apply {
                    put(CommonClientConfigs.GROUP_ID_CONFIG, "lps-oppfolgingsplan-mottak-sendt-sykmelding-01")
                    put(
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer",
                    )
                    put(
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        "org.apache.kafka.common.serialization.StringDeserializer",
                    )
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100")
                }
            return KafkaConsumer<String, String>(kafkaConfig).apply {
                subscribe(listOf(SENDT_SYKMELDING_TOPIC))
            }
        }
    }
}
