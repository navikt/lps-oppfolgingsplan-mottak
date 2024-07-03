package no.nav.syfo.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.KafkaListener
import no.nav.syfo.application.kafka.consumerProperties
import no.nav.syfo.application.kafka.pollDurationInMillis
import no.nav.syfo.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

const val SENDT_SYKMELDING_TOPIC = "teamsykmelding.syfo-sendt-sykmelding"

class SendtSykmeldingAivenConsumer(
    val env: KafkaEnv,
    private val sykmeldingService: SendtSykmeldingService,
) : KafkaListener {
    private val log = LoggerFactory.getLogger(SendtSykmeldingAivenConsumer::class.qualifiedName)
    private val kafkaListener: KafkaConsumer<String, String>
    private val objectMapper = configuredJacksonMapper()

    init {
        val kafkaConfig = consumerProperties(env).apply {
            put(CommonClientConfigs.GROUP_ID_CONFIG, "lps-oppfolgingsplan-mottak-sendt-sykmelding-01")
            put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer"
            )
            put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer"
            )
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
        kafkaListener = KafkaConsumer(kafkaConfig)
        kafkaListener.subscribe(listOf(SENDT_SYKMELDING_TOPIC))
    }

    override suspend fun listen(applicationState: ApplicationState) {
        while (applicationState.ready) {
            kafkaListener.poll(pollDurationInMillis).forEach { record: ConsumerRecord<String, String> ->
                processRecord(record)
            }
        }
    }

    private fun processRecord(record: ConsumerRecord<String, String>) {
        try {
            val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = objectMapper.readValue(record.value())
            val sykmeldingId = record.key()

            if (sykmeldingKafkaMessage == null) {
                sykmeldingService.deleteSykmeldingperioder(sykmeldingId)
            } else {
                sykmeldingService.persistSykmeldingperioder(
                    sykmeldingId = sykmeldingId,
                    employeeIdentificationNumber = sykmeldingKafkaMessage.kafkaMetadata.fnr,
                    orgnumber = sykmeldingKafkaMessage.event.arbeidsgiver.orgnummer,
                    sykmeldingsperioder = sykmeldingKafkaMessage.sykmelding.sykmeldingsperioder
                )
                kafkaListener.commitSync()
            }
        } catch (e: Exception) {
            log.error("Error encountered while processing sykmelding: ${e.message}", e)
        }
    }
}
