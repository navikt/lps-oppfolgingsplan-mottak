package no.nav.syfo.mockdata

import io.ktor.server.application.Application
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.DatabaseInterface

fun Application.testApiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: ApplicationEnvironment,
) {
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val wellKnownMaskinporten = wellKnownInternalAzureAD()
    this.apiModule(
        applicationState = applicationState,
        database = database,
        environment = environment,
        wellKnownMaskinporten = wellKnownMaskinporten,
        wellKnownInternalAzureAD = wellKnownInternalAzureAD,
    )
}
