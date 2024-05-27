package no.nav.syfo.client.dokarkiv

import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.application.environment.UrlEnv
import no.nav.syfo.application.metric.COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_FALSE
import no.nav.syfo.application.metric.COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_TRUE
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.dokarkiv.domain.AvsenderMottaker
import no.nav.syfo.client.dokarkiv.domain.Bruker
import no.nav.syfo.client.dokarkiv.domain.Dokument
import no.nav.syfo.client.dokarkiv.domain.Dokumentvariant
import no.nav.syfo.client.dokarkiv.domain.JournalpostRequest
import no.nav.syfo.client.dokarkiv.domain.JournalpostResponse
import no.nav.syfo.client.dokarkiv.domain.Sak
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.util.createBearerToken
import org.slf4j.LoggerFactory

class DokarkivClient(
    urls: UrlEnv,
    private val azureAdClient: AzureAdClient,
    private val eregClient: EregClient,
) {
    private val baseUrl = urls.dokarkivUrl
    private val scope = urls.dokarkivScope
    private val client = httpClientDefault()
    private val log = LoggerFactory.getLogger(DokarkivClient::class.qualifiedName)

    suspend fun journalforLps(
        followUpPlan: FollowUpPlanDTO,
        employerOrgnr: String,
        pdf: ByteArray,
        uuid: UUID,
    ): String {
        val avsenderMottaker = createAvsenderMottaker(employerOrgnr, employerOrgnr)
        val orgName = eregClient.getEmployerOrganisationName(employerOrgnr) ?: employerOrgnr

        val journalpostRequest = createJournalpostRequest(
            followUpPlan.employeeIdentificationNumber,
            pdf,
            navn = orgName,
            avsenderMottaker,
            "NAV_NO",
            uuid.toString(),
        )
        return sendRequestToDokarkiv(journalpostRequest)
    }

    suspend fun journalforAltinnLps(
        lps: AltinnLpsOppfolgingsplan,
        virksomhetsnavn: String,
    ): String {
        val avsenderMottaker = createAvsenderMottaker(lps.orgnummer, virksomhetsnavn)
        val journalpostRequest = createJournalpostRequest(
            lps.fnr!!,
            lps.pdf!!,
            virksomhetsnavn,
            avsenderMottaker,
            "ALTINN",
            lps.uuid.toString(),
        )
        return sendRequestToDokarkiv(journalpostRequest)
    }

    @Suppress("ThrowsCount")
    private suspend fun sendRequestToDokarkiv(journalpostRequest: JournalpostRequest): String {
        val token = azureAdClient.getSystemToken(scope)?.accessToken
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
            log.error("Could not send LPS plan to dokarkiv", e)
            COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_FALSE.increment()
            throw e
        }

        val responseBody = when (response.status) {
            HttpStatusCode.Created -> {
                COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_TRUE.increment()
                runBlocking {
                    response.body<JournalpostResponse>()
                }

            }

            HttpStatusCode.Conflict -> {
                log.warn("Journalpost for LPS plan already created!")
                runBlocking {
                    response.body<JournalpostResponse>()
                }
            }

            else -> {
                log.error("Call to Dokarkiv failed with status: ${response.status}, : ${response.bodyAsText()}")
                COUNT_METRIKK_FOLLOWUP_LPS_LPS_JOURNALFORT_TIL_GOSYS_FALSE.increment()
                throw RuntimeException("Failed to call back system with status: ${response.status}, : ${response.bodyAsText()}")
            }
        }

        if (!responseBody.journalpostferdigstilt) {
            log.warn("Journalpost is not ferdigstilt with message " + responseBody.melding)
        }
        return responseBody.journalpostId.toString()
    }

    private fun createAvsenderMottaker(
        orgnummer: String,
        virksomhetsnavn: String,
    ) = AvsenderMottaker(
        id = orgnummer,
        idType = ID_TYPE_ORGNR,
        navn = virksomhetsnavn,
    )

    private fun createJournalpostRequest(
        fnr: String,
        pdf: ByteArray,
        navn: String,
        avsenderMottaker: AvsenderMottaker,
        kanal: String,
        uuid: String,
    ): JournalpostRequest {
        val dokumentnavn = "Oppfølgingsplan $navn"
        return JournalpostRequest(
            tema = TEMA_OPP,
            tittel = dokumentnavn,
            journalfoerendeEnhet = JOURNALFORENDE_ENHET,
            journalpostType = JOURNALPOST_TYPE,
            kanal = kanal,
            sak = Sak(sakstype = SAKSTYPE_GENERELL_SAK),
            avsenderMottaker = avsenderMottaker,
            bruker = Bruker(
                id = fnr,
                idType = FNR_TYPE,
            ),
            dokumenter = makeDokumenter(dokumentnavn, pdf),
            eksternReferanseId = uuid,
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
