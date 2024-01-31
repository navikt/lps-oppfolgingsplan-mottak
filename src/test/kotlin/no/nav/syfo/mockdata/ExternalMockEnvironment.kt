package no.nav.syfo.mockdata

import no.nav.syfo.client.wellknown.WellKnown
import java.nio.file.Paths

fun wellKnown(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/test/v2",
        jwksUri = uri.toString(),
    )
}
