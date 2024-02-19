package no.nav.syfo.mockdata

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.mockdata.UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        PERSONIDENT_VEILEDER_NO_ACCESS.value -> respond(Tilgang(erGodkjent = false))
        else -> respond(Tilgang(erGodkjent = true))
    }
}
