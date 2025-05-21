package no.nav.syfo.application.exception

import io.ktor.http.HttpStatusCode

enum class ErrorType {
    AUTHENTICATION_ERROR,
    VALIDATION_ERROR,
    NOT_FOUND,
    INTERNAL_SERVER_ERROR,
    ILLEGAL_ARGUMENT,
    BAD_REQUEST,
    FOLLOWUP_PLAN_NOT_FOUND,
    FORBIDDEN_ACCESS_VEILEDER,
    GENERAL_PRACTITIONER_NOT_FOUND,
    EMPLOYEE_NOT_FOUND,
    NO_ACTIVE_SENT_SYKMELDING,
    NO_ACTIVE_EMPLOYMENT,
    SERVICE_UNAVAILABLE,
}

sealed class ApiError(val status: HttpStatusCode, val type: ErrorType, open val message: String) {
    data class FollowUpPlanDTOValidationError(override val message: String) :
        ApiError(HttpStatusCode.BadRequest, ErrorType.VALIDATION_ERROR, message)

    data class NotFoundError(override val message: String) :
        ApiError(HttpStatusCode.NotFound, ErrorType.NOT_FOUND, message)

    data class InternalServerError(override val message: String) :
        ApiError(HttpStatusCode.InternalServerError, ErrorType.INTERNAL_SERVER_ERROR, message)

    data class IllegalArgumentError(override val message: String) :
        ApiError(HttpStatusCode.BadRequest, ErrorType.ILLEGAL_ARGUMENT, message)

    data class BadRequestError(override val message: String) :
        ApiError(HttpStatusCode.BadRequest, ErrorType.BAD_REQUEST, message)

    data class AuthenticationError(override val message: String) :
        ApiError(HttpStatusCode.Unauthorized, ErrorType.AUTHENTICATION_ERROR, message)

    data class ServiceUnavailableError(override val message: String) :
        ApiError(HttpStatusCode.ServiceUnavailable, ErrorType.SERVICE_UNAVAILABLE, message)

    data object GeneralPractitionerNotFoundError :
        ApiError(
            HttpStatusCode.NotFound,
            ErrorType.GENERAL_PRACTITIONER_NOT_FOUND,
            "General practitioner was not found"
        )

    data object NoActiveSentSykmeldingError :
        ApiError(
            HttpStatusCode.Forbidden,
            ErrorType.NO_ACTIVE_SENT_SYKMELDING,
            "No active sykmelding sent to employer"
        )

    data object NoActiveEmploymentError :
        ApiError(
            HttpStatusCode.Forbidden,
            ErrorType.NO_ACTIVE_EMPLOYMENT,
            "No active employment relationship found for given orgnumber"
        )

    data object EmployeeNotFoundError :
        ApiError(
            HttpStatusCode.NotFound,
            ErrorType.EMPLOYEE_NOT_FOUND,
            "Could not find requested person in our systems"
        )

    data object ForbiddenAccessVeilederError :
        ApiError(
            HttpStatusCode.Forbidden,
            ErrorType.FORBIDDEN_ACCESS_VEILEDER,
            "Forbidden"
        )

    data object FollowupPlanNotFoundError :
        ApiError(
            HttpStatusCode.NotFound,
            ErrorType.FOLLOWUP_PLAN_NOT_FOUND,
            "The follow-up plan with a given uuid was not found"
        )

    data object PdlServiceUnavailableError :
        ApiError(
            HttpStatusCode.ServiceUnavailable,
            ErrorType.SERVICE_UNAVAILABLE,
            "Person lookup service temporarily unavailable. Please try again later."
        )
}
