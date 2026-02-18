package no.nav.syfo.application.exception

class MissingRequiredVariableException(
    varName: String,
    cause: Throwable? = null,
) : RuntimeException("Missing required variable \"$varName\"", cause)
