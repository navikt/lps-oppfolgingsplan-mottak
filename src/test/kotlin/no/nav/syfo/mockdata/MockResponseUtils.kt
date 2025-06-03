package no.nav.syfo.mockdata

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.syfo.client.azuread.AzureAdTokenResponse
import no.nav.syfo.client.krrproxy.domain.Kontaktinfo
import no.nav.syfo.client.krrproxy.domain.PostPersonerResponse
import no.nav.syfo.client.pdl.domain.Adressebeskyttelse
import no.nav.syfo.client.pdl.domain.Bostedsadresse
import no.nav.syfo.client.pdl.domain.Gradering
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.PdlPerson
import no.nav.syfo.client.pdl.domain.PdlPersonResponse
import no.nav.syfo.client.pdl.domain.PersonNavn
import no.nav.syfo.client.pdl.domain.Vegadresse
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.mockdata.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.configuredJacksonMapper

val mapper = configuredJacksonMapper()

fun <T> MockRequestHandleScope.respond(body: T): HttpResponseData =
    respond(
        mapper.writeValueAsString(body),
        HttpStatusCode.OK,
        headersOf(HttpHeaders.ContentType, "application/json")
    )

fun MockRequestHandleScope.azureAdMockResponse(): HttpResponseData = respond(
    AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type",
    )
)

fun MockRequestHandleScope.pdlPersonResponse(): HttpResponseData {
    return respond(
        (PdlPersonResponse(
            errors = null, data = PdlHentPerson(
                hentPerson = PdlPerson(
                    adressebeskyttelse = listOf(Adressebeskyttelse(Gradering.UGRADERT)),
                    navn = listOf(PersonNavn("Mavn", "Mellom", "Etternavn")),
                    bostedsadresse = listOf(Bostedsadresse(Vegadresse("Gate", "1", "A", "1234")))
                )
            )
        ))
    )
}

fun MockRequestHandleScope.krrResponse(): HttpResponseData {
    return respond(
        PostPersonerResponse(
            personer = mapOf(
                ARBEIDSTAKER_FNR to Kontaktinfo(
                    kanVarsles = true,
                    reservert = false,
                    mobiltelefonnummer = "12121212",
                    epostadresse = "epost@some.no"
                )
            )
        )
    )
}

fun MockRequestHandleScope.opPdfGenResponse(): HttpResponseData {
    return respond("<MOCK PDF CONTENT>".toByteArray())
}

fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value -> respond(Tilgang(erGodkjent = false))
        else -> respond(Tilgang(erGodkjent = true))
    }
}
