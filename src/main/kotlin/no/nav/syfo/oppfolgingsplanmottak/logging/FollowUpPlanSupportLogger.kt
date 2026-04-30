package no.nav.syfo.oppfolgingsplanmottak.logging

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.syfo.util.configuredJacksonMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.HexFormat

private const val REDACTED_VALUE = "[REDACTED]"
private const val NOT_VALIDATED = "not_validated"
private const val PARSE_RESULT_RECEIVED = "received"
private const val PARSE_RESULT_FAILED = "failed"
private const val PARSE_RESULT_PARSED = "parsed"
private const val VALIDATION_RESULT_FAILED = "failed"

private const val EVENT_TYPE_RECEIVED = "follow_up_plan_received"
private const val EVENT_TYPE_PARSE_FAILED = "follow_up_plan_parse_failed"
private const val EVENT_TYPE_VALIDATION_FAILED = "follow_up_plan_validation_failed"

data class FollowUpPlanSupportLogData(
    val eventType: String,
    val callId: String,
    val planUuid: String,
    val consumerClientId: String?,
    val organizationNumber: String,
    val lpsOrgnumber: String,
    val payloadSizeBytes: Int,
    val payloadSha256: String,
    val sanitizedPayload: String?,
    val parseResult: String,
    val validationResult: String,
    val errorMessage: String? = null,
)

interface FollowUpPlanSupportLogger {
    @Suppress("LongParameterList")
    fun logReceived(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
    )

    @Suppress("LongParameterList")
    fun logParseFailed(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
        errorMessage: String,
    )

    @Suppress("LongParameterList")
    fun logValidationFailed(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
        errorMessage: String,
    )
}

class TeamLogsFollowUpPlanSupportLogger(
    private val logger: Logger = LoggerFactory.getLogger(TeamLogsFollowUpPlanSupportLogger::class.qualifiedName),
) : FollowUpPlanSupportLogger {
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")

    override fun logReceived(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
    ) {
        logToTeamLogs(
            createFollowUpPlanSupportLogData(
                eventType = EVENT_TYPE_RECEIVED,
                rawPayload = rawPayload,
                callId = callId,
                planUuid = planUuid,
                consumerClientId = consumerClientId,
                organizationNumber = organizationNumber,
                lpsOrgnumber = lpsOrgnumber,
                parseResult = PARSE_RESULT_RECEIVED,
                validationResult = NOT_VALIDATED,
            ),
        )
    }

    override fun logParseFailed(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
        errorMessage: String,
    ) {
        logToTeamLogs(
            createFollowUpPlanSupportLogData(
                eventType = EVENT_TYPE_PARSE_FAILED,
                rawPayload = rawPayload,
                callId = callId,
                planUuid = planUuid,
                consumerClientId = consumerClientId,
                organizationNumber = organizationNumber,
                lpsOrgnumber = lpsOrgnumber,
                parseResult = PARSE_RESULT_FAILED,
                validationResult = NOT_VALIDATED,
                errorMessage = errorMessage,
            ),
        )
    }

    override fun logValidationFailed(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
        errorMessage: String,
    ) {
        logToTeamLogs(
            createFollowUpPlanSupportLogData(
                eventType = EVENT_TYPE_VALIDATION_FAILED,
                rawPayload = rawPayload,
                callId = callId,
                planUuid = planUuid,
                consumerClientId = consumerClientId,
                organizationNumber = organizationNumber,
                lpsOrgnumber = lpsOrgnumber,
                parseResult = PARSE_RESULT_PARSED,
                validationResult = VALIDATION_RESULT_FAILED,
                errorMessage = errorMessage,
            ),
        )
    }

    private fun logToTeamLogs(logData: FollowUpPlanSupportLogData) {
        logger.info(
            teamLogsMarker,
            "Follow-up plan support event {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
            keyValue("event_type", logData.eventType),
            keyValue("callId", logData.callId),
            keyValue("plan_uuid", logData.planUuid),
            keyValue("consumer_client_id", logData.consumerClientId),
            keyValue("organization_number", logData.organizationNumber),
            keyValue("lps_orgnumber", logData.lpsOrgnumber),
            keyValue("payload_size_bytes", logData.payloadSizeBytes),
            keyValue("payload_sha256", logData.payloadSha256),
            keyValue("sanitized_payload", logData.sanitizedPayload),
            keyValue("parse_result", logData.parseResult),
            keyValue("validation_result", logData.validationResult),
            keyValue("error_message", logData.errorMessage),
        )
    }
}

@Suppress("LongParameterList")
internal fun createFollowUpPlanSupportLogData(
    eventType: String,
    rawPayload: String,
    callId: String,
    planUuid: String,
    consumerClientId: String?,
    organizationNumber: String,
    lpsOrgnumber: String,
    parseResult: String,
    validationResult: String,
    errorMessage: String? = null,
) = FollowUpPlanSupportLogData(
    eventType = eventType,
    callId = callId,
    planUuid = planUuid,
    consumerClientId = consumerClientId,
    organizationNumber = organizationNumber,
    lpsOrgnumber = lpsOrgnumber,
    payloadSizeBytes = rawPayload.toByteArray(UTF_8).size,
    payloadSha256 = sha256(rawPayload),
    sanitizedPayload = rawPayload.toSanitizedPayload(),
    parseResult = parseResult,
    validationResult = validationResult,
    errorMessage = errorMessage,
)

private fun sha256(rawPayload: String): String =
    HexFormat.of().formatHex(
        MessageDigest
            .getInstance("SHA-256")
            .digest(rawPayload.toByteArray(UTF_8)),
    )

private fun String.toSanitizedPayload(): String? =
    try {
        configuredJacksonMapper().readTree(this).sanitizeJsonNode().toString()
    } catch (_: Exception) {
        null
    }

private fun JsonNode.sanitizeJsonNode(): JsonNode =
    when {
        isObject -> sanitizeObjectNode()
        isArray -> sanitizeArrayNode()
        isBoolean || isNull -> deepCopy()
        else -> JsonNodeFactory.instance.textNode(REDACTED_VALUE)
    }

private fun JsonNode.sanitizeObjectNode(): ObjectNode =
    JsonNodeFactory.instance.objectNode().also { objectNode ->
        properties().forEach { (key, value) ->
            objectNode.set<JsonNode>(key, value.sanitizeJsonNode())
        }
    }

private fun JsonNode.sanitizeArrayNode(): ArrayNode =
    JsonNodeFactory.instance.arrayNode().also { arrayNode ->
        elements().forEachRemaining { element ->
            arrayNode.add(element.sanitizeJsonNode())
        }
    }
