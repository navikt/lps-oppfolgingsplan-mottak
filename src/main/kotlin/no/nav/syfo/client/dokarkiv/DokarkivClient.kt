package no.nav.syfo.client.dokarkiv

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.dokarkiv.domain.*
import no.nav.syfo.util.createBearerToken
import org.slf4j.LoggerFactory

class DokarkivClient(
    urls: UrlEnv,
    private val azureAdClient: AzureAdClient,
) {
    private val baseUrl = urls.dokarkivUrl
    private val scope = urls.dokarkivScope
    private val client = httpClientDefault()
    private val log = LoggerFactory.getLogger(DokarkivClient::class.qualifiedName)

    suspend fun journalforAltinnLps(
        lps: AltinnLpsOppfolgingsplan,
        virksomhetsnavn: String,
    ): String {
        val avsenderMottaker = createAvsenderMottaker(lps, virksomhetsnavn)
        val journalpostRequest = createJournalpostRequest(
            lps,
            virksomhetsnavn,
            avsenderMottaker,
            KANAL_TYPE_ALTINN,
        )
        return sendRequestToDokarkiv(journalpostRequest)
    }

    @Suppress("ThrowsCount")
    private suspend fun sendRequestToDokarkiv(journalpostRequest: JournalpostRequest): String {
        val token = azureAdClient.getSystemToken(scope) ?.accessToken
            ?: throw RuntimeException("Failed to Journalfor: No token was found")
        val requestUrl = baseUrl + JOURNALPOST_API_PATH
        val response = try {
            client.post(requestUrl) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append(HttpHeaders.Authorization, createBearerToken(token))
                }
                setBody(journalpostRequest)
            }
        } catch (e: Exception) {
            log.error("Could not send Altinn-LPS to dokarkiv", e)
            throw e
        }

        val responseBody = when (response.status) {
            HttpStatusCode.Created -> {
                runBlocking {
                    response.body<JournalpostResponse>()
                }
            }

            HttpStatusCode.Conflict -> {
                log.warn("Journalpost for Altinn-LPS already created!")
                runBlocking {
                    response.body<JournalpostResponse>()
                }
            }

            else -> {
                log.error("Call to dokarkiv failed with status: ${response.status}")
                throw RuntimeException("Failed to call dokarkiv")
            }
        }

        if (!responseBody.journalpostferdigstilt) {
            log.warn("Journalpost is not ferdigstilt with message " + responseBody.melding)
        }
        return responseBody.journalpostId.toString()
    }

    private fun createAvsenderMottaker(
        lps: AltinnLpsOppfolgingsplan,
        virksomhetsnavn: String
    ) = AvsenderMottaker(
        id = lps.orgnummer,
        idType = ID_TYPE_ORGNR,
        navn = virksomhetsnavn,
    )

    private fun createJournalpostRequest(
        lps: AltinnLpsOppfolgingsplan,
        virksomhetsnavn: String,
        avsenderMottaker: AvsenderMottaker,
        kanal: String,
    ): JournalpostRequest {
        val dokumentnavn = "Oppfølgingsplan $virksomhetsnavn"
        return JournalpostRequest(
            tema = TEMA_OPP,
            tittel = dokumentnavn,
            journalfoerendeEnhet = JOURNALFORENDE_ENHET,
            journalpostType = JOURNALPOST_TYPE,
            kanal = kanal,
            sak = Sak(sakstype = SAKSTYPE_GENERELL_SAK),
            avsenderMottaker = avsenderMottaker,
            bruker = Bruker(
                id = lps.fnr!!,
                idType = FNR_TYPE,
            ),
            dokumenter = makeDokumenter(dokumentnavn, lps.pdf!!)
        )
    }

    private fun makeDokumenter(
        dokumentNavn: String,
        dokumentPdf: ByteArray,
    ) = listOf(
        Dokument(
            dokumentKategori = DOKUMENT_KATEGORY_ES,
            brevkode = BREV_KODE_TYPE_OPPF_PLA,
            tittel = dokumentNavn,
            dokumentvarianter = listOf(
                Dokumentvariant(
                    filnavn = dokumentNavn,
                    filtype = FILE_TYPE_PDFA,
                    variantformat = FORMAT_TYPE_ARKIV,
                    fysiskDokument = dokumentPdf,
                )
            )
        )
    )

    companion object {
        const val KANAL_TYPE_ALTINN = "ALTINN"
        const val ID_TYPE_ORGNR = "ORGNR"
        const val TEMA_OPP = "OPP"
        const val SAKSTYPE_GENERELL_SAK = "GENERELL_SAK"
        const val FNR_TYPE = "FNR"
        const val FILE_TYPE_PDFA = "PDFA"
        const val FORMAT_TYPE_ARKIV = "ARKIV"
        const val DOKUMENT_KATEGORY_ES = "ES"
        const val BREV_KODE_TYPE_OPPF_PLA = "OPPF_PLA"
        const val JOURNALFORENDE_ENHET = 9999
        const val JOURNALPOST_TYPE = "INNGAAENDE"

        const val JOURNALPOST_API_PATH = "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
    }
}
