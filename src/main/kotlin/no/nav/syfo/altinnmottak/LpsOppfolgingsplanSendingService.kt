package no.nav.syfo.altinnmottak

import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import org.slf4j.LoggerFactory
import java.io.Serializable

class LpsOppfolgingsplanSendingService(
    private val opPdfGenConsumer: OpPdfGenClient,
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val dokarkivConsumer: DokarkivClient,
    private val toggles: ToggleEnv,
) {
    private val log = LoggerFactory.getLogger(LpsOppfolgingsplanSendingService::class.qualifiedName)

    suspend fun sendLpsPlan(oppfolgingsplanDTO: FollowUpPlanDTO): OppfolgingsplanResponse {
        val sykmeldtFnr = oppfolgingsplanDTO.employeeIdentificationNumber

        var sentToFastlegeId: String? = null
        var sentToNavId: String? = null

        if (toggles.sendLpsPlanToFastlegeToggle && oppfolgingsplanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeId = isdialogmeldingConsumer.sendLpsPlanToFastlege(
                sykmeldtFnr,
                "<MOCK PDF CONTENT>".toByteArray(),
            )
        } else if (toggles.sendLpsPlanToNavToggle && oppfolgingsplanDTO.sendPlanToNav) {
            // TODO: isPersonoppgaveClient.sendLpsPlanToNav()
            sentToNavId = null
        }
        log.warn("Sending plan to fastlege, fnr: $sykmeldtFnr")
        return OppfolgingsplanResponse(sykmeldtFnr = sykmeldtFnr, sentToFastlegeId = sentToFastlegeId, sentToNavId = sentToNavId)
    }

    suspend fun sendLpsPlanDummy() {
        isdialogmeldingConsumer.sendAltinnLpsPlanToFastlege(
            "12121212121",
            "<MOCK PDF CONTENT>".toByteArray(),
        )
    }
}

data class OppfolgingsplanResponse(
    val sykmeldtFnr: String,
    val sentToFastlegeId: String?,
    val sentToNavId: String?,
) : Serializable
