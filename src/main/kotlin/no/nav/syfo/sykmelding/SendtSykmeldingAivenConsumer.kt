package no.nav.syfo.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import net.logstash.logback.argument.StructuredArguments.kv
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
private const val PROCESSING_FAILED_EVENT = "sendt_sykmelding_processing_failed"
private const val PROCESSING_OPERATION = "behandle_sendt_sykmelding"
private const val MAX_SAFE_CAUSE_DEPTH = 10
private val SAFE_EXCEPTION_TYPE = Regex("^[A-Za-z_$][A-Za-z0-9_$]{0,119}$")

internal enum class SendtSykmeldingErrorCode {
    SYKMELDING_DESERIALIZATION_FAILED,
    SYKMELDING_DELETE_FAILED,
    SYKMELDING_PERSIST_FAILED,
    KAFKA_OFFSET_COMMIT_FAILED,
}

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
                    processRecord(record)
                }
            }
        } finally {
            kafkaListener.close(CloseOptions.timeout(KAFKA_CLOSE_TIMEOUT))
        }
    }

    private fun poll(): ConsumerRecords<String, String> = kafkaListener.poll(pollDurationInMillis)

    internal fun processRecord(record: ConsumerRecord<String, String?>) {
        var errorCode = SendtSykmeldingErrorCode.SYKMELDING_DESERIALIZATION_FAILED
        try {
            val sykmeldingKafkaMessage: SykmeldingKafkaMessage? =
                record.value()?.let { objectMapper.readValue(it) }
            val sykmeldingId = record.key()

            if (sykmeldingKafkaMessage == null) {
                errorCode = SendtSykmeldingErrorCode.SYKMELDING_DELETE_FAILED
                sykmeldingService.deleteSykmeldingsperioder(sykmeldingId)
            } else {
                errorCode = SendtSykmeldingErrorCode.SYKMELDING_PERSIST_FAILED
                sykmeldingService.persistSykmeldingsperioder(
                    sykmeldingId = sykmeldingId,
                    employeeIdentificationNumber = sykmeldingKafkaMessage.kafkaMetadata.fnr,
                    orgnumber = sykmeldingKafkaMessage.event.arbeidsgiver.orgnummer,
                    sykmeldingsperioder = sykmeldingKafkaMessage.sykmelding.sykmeldingsperioder,
                )
            }
            errorCode = SendtSykmeldingErrorCode.KAFKA_OFFSET_COMMIT_FAILED
            kafkaListener.commitSync()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            log.error(
                "Kunne ikke behandle sendt sykmelding: {} {} {} {} {}",
                kv("event_type", PROCESSING_FAILED_EVENT),
                kv("error_code", errorCode.name),
                kv("operation", PROCESSING_OPERATION),
                kv("exception_type", e.safeExceptionType()),
                kv("root_cause_type", e.safeRootCauseType()),
                e.withoutDynamicMessages(),
            )
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

private fun Throwable.safeExceptionType(): String = javaClass.simpleName.takeIf { it.matches(SAFE_EXCEPTION_TYPE) } ?: "UnknownException"

private fun Throwable.safeRootCauseType(): String {
    var rootCause = this
    repeat(MAX_SAFE_CAUSE_DEPTH) {
        rootCause = rootCause.cause?.takeUnless { it === rootCause } ?: return rootCause.safeExceptionType()
    }
    return rootCause.safeExceptionType()
}

private fun Throwable.withoutDynamicMessages(depth: Int = 0): Throwable =
    RuntimeException(
        safeExceptionType(),
        cause
            ?.takeUnless { it === this || depth >= MAX_SAFE_CAUSE_DEPTH }
            ?.withoutDynamicMessages(depth + 1),
    ).also { safeThrowable ->
        safeThrowable.stackTrace = stackTrace
    }
