package no.nav.syfo.application.exception

import io.ktor.server.plugins.BadRequestException

class EmployeeNotFoundException(message: String) : BadRequestException(message)
