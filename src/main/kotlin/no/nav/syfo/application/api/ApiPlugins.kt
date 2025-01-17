package no.nav.syfo.application.api

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import no.nav.syfo.application.exception.ApiError
import no.nav.syfo.application.exception.ApiError.BadRequestError
import no.nav.syfo.application.exception.ApiError.EmployeeNotFoundError
import no.nav.syfo.application.exception.ApiError.FollowUpPlanDTOValidationError
import no.nav.syfo.application.exception.ApiError.ForbiddenAccessVeilederError
import no.nav.syfo.application.exception.ApiError.GeneralPractitionerNotFoundError
import no.nav.syfo.application.exception.ApiError.IllegalArgumentError
import no.nav.syfo.application.exception.ApiError.InternalServerError
import no.nav.syfo.application.exception.ApiError.NoActiveSentSykmeldingError
import no.nav.syfo.application.exception.ApiError.NotFoundError
import no.nav.syfo.application.exception.EmployeeNotFoundException
import no.nav.syfo.application.exception.FollowUpPlanDTOValidationException
import no.nav.syfo.application.exception.ForbiddenAccessVeilederException
import no.nav.syfo.application.exception.GpNotFoundException
import no.nav.syfo.application.exception.NoActiveEmploymentException
import no.nav.syfo.application.exception.NoActiveSentSykmeldingException
import no.nav.syfo.application.metric.METRICS_REGISTRY
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.configure
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getConsumerClientId
import java.time.Duration
import java.util.*

const val MAX_EXPECTED_VALUE_METRICS = 20L

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson { configure() }
    }
}

fun Application.installMetrics() {
    install(MicrometerMetrics) {
        registry = METRICS_REGISTRY
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .maximumExpectedValue(Duration.ofSeconds(MAX_EXPECTED_VALUE_METRICS).toNanos().toDouble())
            .build()
    }
}

fun Application.installCallId() {
    install(CallId) {
        retrieve { it.request.headers[NAV_CALL_ID_HEADER] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }
}

private fun logException(call: ApplicationCall, cause: Throwable) {
    val callId = call.getCallId()
    val consumerClientId = call.getConsumerClientId()
    val logExceptionMessage = "Caught exception, callId=$callId, consumerClientId=$consumerClientId"
    val log = call.application.log
    when (cause) {
        is ForbiddenAccessVeilederException -> log.warn(logExceptionMessage, cause)
        is GpNotFoundException -> log.warn(logExceptionMessage, cause)
        else -> log.error(logExceptionMessage, cause)
    }
}

private fun determineApiError(cause: Throwable): ApiError {
    return when (cause) {
        is FollowUpPlanDTOValidationException -> FollowUpPlanDTOValidationError(
            cause.message ?: "DTO validation failed"
        )

        is EmployeeNotFoundException -> EmployeeNotFoundError
        is GpNotFoundException -> GeneralPractitionerNotFoundError
        is NoActiveSentSykmeldingException -> NoActiveSentSykmeldingError
        is NoActiveEmploymentException -> ApiError.NoActiveEmploymentError
        is ForbiddenAccessVeilederException -> ForbiddenAccessVeilederError
        is BadRequestException -> BadRequestError(cause.message ?: "Bad request")
        is IllegalArgumentException -> IllegalArgumentError(cause.message ?: "Illegal argument")
        is NotFoundException -> NotFoundError(cause.message ?: "Not found")
        else -> InternalServerError(cause.message ?: "Internal server error")
    }
}

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logException(call, cause)
            val apiError = determineApiError(cause)
            call.respond(apiError.status, apiError)
        }
    }
}
