package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface

const val POD_LIVENESS_PATH = "/internal/is_alive"
const val POD_READINESS_PATH = "/internal/is_ready"

fun Routing.registerPodApi(
    applicationState: ApplicationState,
    database: DatabaseInterface,
) {
    get(POD_LIVENESS_PATH) {
        if (applicationState.alive) {
            call.respondText("I'm alive! :)")
        } else {
            call.respondText("I'm dead x_x", status = HttpStatusCode.InternalServerError)
        }
    }
    get(POD_READINESS_PATH) {
        if (isReady(applicationState, database)) {
            call.respondText("I'm ready! :)")
        } else {
            call.respondText("Please wait! I'm not ready :(", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun isReady(applicationState: ApplicationState, database: DatabaseInterface): Boolean {
    return applicationState.ready && database.isOk()
}

@Suppress("SwallowedException")
private fun DatabaseInterface.isOk(): Boolean {
    return try {
        connection.use {
            it.isValid(1)
        }
    } catch (ex: Exception) {
        false
    }
}
