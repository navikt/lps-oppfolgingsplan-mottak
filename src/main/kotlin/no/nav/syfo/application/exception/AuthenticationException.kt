package no.nav.syfo.application.exception

class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
