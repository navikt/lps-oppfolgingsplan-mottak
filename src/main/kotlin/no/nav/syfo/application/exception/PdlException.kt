package no.nav.syfo.application.exception

import io.ktor.http.HttpStatusCode


/*
* Exception classes for PDL client errors
*/
sealed class PdlException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class PdlNotFoundException(message: String) : PdlException(message)
class PdlBadRequestException(message: String) : PdlException(message)
class PdlServerException(message: String) : PdlException(message)
class PdlUnauthorizedException(message: String, val policy: String? = null) : PdlException(message)
class PdlHttpException(message: String, val status: HttpStatusCode) : PdlException(message)
class PdlGenericException(message: String, cause: Throwable? = null) : PdlException(message, cause)


/**
 * Exception for errors related to PDL service availability or functionality.
 * This represents temporary service issues rather than data not existing.
 */
class PdlServiceException(message: String) : RuntimeException(message)
