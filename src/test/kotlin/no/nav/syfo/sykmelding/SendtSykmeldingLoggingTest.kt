package no.nav.syfo.sykmelding

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.logstash.logback.encoder.LogstashEncoder
import no.nav.syfo.sykmelding.domain.ArbeidsgiverStatusKafkaDTO
import no.nav.syfo.sykmelding.domain.ArbeidsgiverSykmelding
import no.nav.syfo.sykmelding.domain.KafkaMetadataDTO
import no.nav.syfo.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.sykmelding.domain.SykmeldingStatusKafkaEventDTO
import no.nav.syfo.sykmelding.domain.SykmeldingsperiodeAGDTO
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.LocalDate

private const val TRACE_ID = "0123456789abcdef0123456789abcdef"
private const val SENSITIVE_KEY = "sykmelding-id-12345678901"
private const val SENSITIVE_EMAIL = "someone@example.com"
private const val SENSITIVE_URL = "https://example.test/person/123"
private const val SENSITIVE_TOKEN = "Bearer sensitive-token"

class SendtSykmeldingLoggingTest :
    FunSpec({
        test("feilkodekatalogen er lukket og følger loggkontrakten") {
            val errorCodes = SendtSykmeldingErrorCode.entries.map { it.name }

            errorCodes shouldBe
                listOf(
                    "SYKMELDING_DESERIALIZATION_FAILED",
                    "SYKMELDING_DELETE_FAILED",
                    "SYKMELDING_PERSIST_FAILED",
                    "KAFKA_OFFSET_COMMIT_FAILED",
                )
            errorCodes.forEach { errorCode ->
                errorCode shouldBe errorCode.uppercase()
                errorCode.matches(Regex("^[A-Z][A-Z0-9_]{0,79}$")) shouldBe true
            }
        }

        test("deserialiseringsfeil logger én trygg terminalhendelse") {
            val (consumer, _, kafkaConsumer) = nyConsumer()
            val record = record("{\"fnr\":\"12345678901\",\"url\":\"$SENSITIVE_URL\"")

            val loggmeldinger = fangLogg { consumer.processRecord(record) }

            loggmeldinger.verifiserTryggFeil("SYKMELDING_DESERIALIZATION_FAILED")
            verify(exactly = 0) { kafkaConsumer.commitSync() }
        }

        test("feil ved sletting av tombstone logger én trygg terminalhendelse") {
            val (consumer, service, kafkaConsumer) = nyConsumer()
            every { service.deleteSykmeldingsperioder(SENSITIVE_KEY) } throws sensitivFeil()

            val loggmeldinger = fangLogg { consumer.processRecord(record(null)) }

            loggmeldinger.verifiserTryggFeil("SYKMELDING_DELETE_FAILED")
            verify(exactly = 0) { kafkaConsumer.commitSync() }
        }

        test("feil ved lagring logger én trygg terminalhendelse") {
            val (consumer, service, kafkaConsumer) = nyConsumer()
            every {
                service.persistSykmeldingsperioder(
                    sykmeldingId = any(),
                    employeeIdentificationNumber = any(),
                    orgnumber = any(),
                    sykmeldingsperioder = any(),
                )
            } throws sensitivFeil()

            val loggmeldinger = fangLogg { consumer.processRecord(record(gyldigPayload())) }

            loggmeldinger.verifiserTryggFeil("SYKMELDING_PERSIST_FAILED")
            verify(exactly = 0) { kafkaConsumer.commitSync() }
        }

        test("feil ved offset-commit logger én trygg terminalhendelse") {
            val (consumer, _, kafkaConsumer) = nyConsumer()
            every { kafkaConsumer.commitSync() } throws sensitivFeil()

            val loggmeldinger = fangLogg { consumer.processRecord(record(null)) }

            loggmeldinger.verifiserTryggFeil("KAFKA_OFFSET_COMMIT_FAILED")
            verify(exactly = 1) { kafkaConsumer.commitSync() }
        }

        test("vellykket record gir ingen per-record info og beholder commit-semantikken") {
            val (consumer, service, kafkaConsumer) = nyConsumer()

            val loggmeldinger = fangLogg { consumer.processRecord(record(null)) }

            loggmeldinger.shouldBeEmpty()
            verify(exactly = 1) { service.deleteSykmeldingsperioder(SENSITIVE_KEY) }
            verify(exactly = 1) { kafkaConsumer.commitSync() }
        }

        test("cancellation propageres uendret uten feillogg eller commit") {
            val (consumer, service, kafkaConsumer) = nyConsumer()
            val cancellation = CancellationException("$SENSITIVE_KEY $SENSITIVE_TOKEN")
            every { service.deleteSykmeldingsperioder(SENSITIVE_KEY) } throws cancellation

            val resultat = fangLoggMedResult { consumer.processRecord(record(null)) }

            resultat.failure shouldBe cancellation
            resultat.loggmeldinger.shouldBeEmpty()
            verify(exactly = 0) { kafkaConsumer.commitSync() }
        }
    })

private data class FangetLoggresultat(
    val failure: Throwable?,
    val loggmeldinger: List<ILoggingEvent>,
)

private fun nyConsumer(): Triple<SendtSykmeldingAivenConsumer, SendtSykmeldingService, Consumer<String, String>> {
    val kafkaConsumer = mockk<Consumer<String, String>>(relaxed = true)
    val service = mockk<SendtSykmeldingService>(relaxed = true)
    return Triple(
        SendtSykmeldingAivenConsumer(
            kafkaListener = kafkaConsumer,
            sykmeldingService = service,
        ),
        service,
        kafkaConsumer,
    )
}

private fun record(value: String?): ConsumerRecord<String, String?> =
    ConsumerRecord(
        SENDT_SYKMELDING_TOPIC,
        0,
        0,
        SENSITIVE_KEY,
        value,
    )

private fun gyldigPayload(): String =
    configuredJacksonMapper().writeValueAsString(
        SykmeldingKafkaMessage(
            sykmelding =
                ArbeidsgiverSykmelding(
                    sykmeldingsperioder =
                        listOf(
                            SykmeldingsperiodeAGDTO(
                                fom = LocalDate.parse("2026-01-01"),
                                tom = LocalDate.parse("2026-01-31"),
                            ),
                        ),
                ),
            kafkaMetadata =
                KafkaMetadataDTO(
                    sykmeldingId = SENSITIVE_KEY,
                    fnr = "12345678901",
                ),
            event =
                SykmeldingStatusKafkaEventDTO(
                    sykmeldingId = SENSITIVE_KEY,
                    arbeidsgiver =
                        ArbeidsgiverStatusKafkaDTO(
                            orgnummer = "999999999",
                            orgNavn = "$SENSITIVE_EMAIL $SENSITIVE_URL",
                        ),
                ),
        ),
    )

private fun sensitivFeil(): Exception =
    IllegalStateException(
        "$SENSITIVE_KEY $SENSITIVE_EMAIL $SENSITIVE_URL $SENSITIVE_TOKEN",
    )

private fun fangLogg(block: () -> Unit): List<ILoggingEvent> {
    val resultat = fangLoggMedResult(block)
    resultat.failure?.let { throw it }
    return resultat.loggmeldinger
}

private fun fangLoggMedResult(block: () -> Unit): FangetLoggresultat {
    val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val loggmeldinger =
        ListAppender<ILoggingEvent>().apply {
            start()
            logger.addAppender(this)
        }
    MDC.put("trace_id", TRACE_ID)
    return try {
        var failure: Throwable? = null
        try {
            block()
        } catch (throwable: Throwable) {
            failure = throwable
        }
        FangetLoggresultat(failure, loggmeldinger.list.toList())
    } finally {
        MDC.remove("trace_id")
        logger.detachAppender(loggmeldinger)
        loggmeldinger.stop()
    }
}

private fun List<ILoggingEvent>.verifiserTryggFeil(errorCode: String) {
    size shouldBe 1
    single().level shouldBe Level.ERROR

    val serialisert = single().serialisertJson()
    serialisert.verdi("event_type") shouldBe "sendt_sykmelding_processing_failed"
    serialisert.verdi("error_code") shouldBe errorCode
    serialisert.verdi("operation") shouldBe "behandle_sendt_sykmelding"
    serialisert.verdi("message") shouldContain "Kunne ikke behandle sendt sykmelding"
    serialisert.verdi("trace_id") shouldBe TRACE_ID
    serialisert.verdi("trace_id").matches(Regex("^[0-9a-f]{32}$")) shouldBe true
    serialisert shouldContainKey "stack_trace"
    serialisert.verdi("stack_trace") shouldContain "SendtSykmeldingLoggingTest"
    MDC.get("trace_id") shouldBe null

    val json = serialisert.toString()
    listOf(
        SENSITIVE_KEY,
        "12345678901",
        SENSITIVE_EMAIL,
        SENSITIVE_URL,
        SENSITIVE_TOKEN,
    ).forEach { canary ->
        json shouldNotContain canary
    }
}

private fun ILoggingEvent.serialisertJson(): JsonObject {
    val encoder =
        LogstashEncoder().apply {
            context = LoggerFactory.getILoggerFactory() as LoggerContext
            start()
        }
    return try {
        Json.parseToJsonElement(encoder.encode(this).decodeToString()).jsonObject
    } finally {
        encoder.stop()
    }
}

private fun JsonObject.verdi(felt: String): String = getValue(felt).jsonPrimitive.content
