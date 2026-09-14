package no.nav.syfo.oppfolgingsplanmottak.logging

class RecordingFollowUpPlanSupportLogger : FollowUpPlanSupportLogger {
    val events = mutableListOf<FollowUpPlanSupportLogData>()

    override fun logReceived(
        rawPayload: String,
        callId: String,
        planUuid: String,
        consumerClientId: String?,
        organizationNumber: String,
        lpsOrgnumber: String,
    ) {
        events.add(
            createFollowUpPlanSupportLogData(
                eventType = "follow_up_plan_received",
                rawPayload = rawPayload,
                callId = callId,
                planUuid = planUuid,
                consumerClientId = consumerClientId,
                organizationNumber = organizationNumber,
                lpsOrgnumber = lpsOrgnumber,
                parseResult = "received",
                validationResult = "not_validated",
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
        events.add(
            createFollowUpPlanSupportLogData(
                eventType = "follow_up_plan_parse_failed",
                rawPayload = rawPayload,
                callId = callId,
                planUuid = planUuid,
                consumerClientId = consumerClientId,
                organizationNumber = organizationNumber,
                lpsOrgnumber = lpsOrgnumber,
                parseResult = "failed",
                validationResult = "not_validated",
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
        events.add(
            createFollowUpPlanSupportLogData(
                eventType = "follow_up_plan_validation_failed",
                rawPayload = rawPayload,
                callId = callId,
                planUuid = planUuid,
                consumerClientId = consumerClientId,
                organizationNumber = organizationNumber,
                lpsOrgnumber = lpsOrgnumber,
                parseResult = "parsed",
                validationResult = "failed",
                errorMessage = errorMessage,
            ),
        )
    }
}
