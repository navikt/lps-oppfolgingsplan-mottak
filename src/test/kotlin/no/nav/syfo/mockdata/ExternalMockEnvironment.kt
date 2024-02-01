package no.nav.syfo.mockdata

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.environment.getEnv
import no.nav.syfo.client.wellknown.WellKnown
import java.nio.file.Paths

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString(),
    )
}

fun wellKnownMaskinporten(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/maskinporten/v2",
        jwksUri = uri.toString(),
    )
}

class ExternalMockEnvironment private constructor() {

    val applicationState: ApplicationState = ApplicationState(alive = true, ready = true)
    val environment = getEnv()
    val wellKnownMaskinporten = wellKnownMaskinporten()
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
