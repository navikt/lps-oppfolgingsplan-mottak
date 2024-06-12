package no.nav.syfo.application.exception

import io.ktor.server.plugins.BadRequestException

class FollowUpPlanDTOValidationException(message: String) : BadRequestException(message)
