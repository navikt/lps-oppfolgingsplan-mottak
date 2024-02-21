package no.nav.syfo.service

import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import java.io.Serializable

class LpsOppfolgingsplanSendingService(
    private val opPdfGenConsumer: OpPdfGenClient,
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val dokarkivConsumer: DokarkivClient,
    private val toggles: ToggleEnv,
) {
    suspend fun sendLpsPlan(oppfolgingsplanDTO: FollowUpPlanDTO): LpsOppfolgingsplanResponse {
        val sykmeldtFnr = oppfolgingsplanDTO.employeeIdentificationNumber
        // TODO:       val pdf = opPdfGenConsumer.generatedPdfResponse("new model")

        var sentToFastlegeId: String? = null
        var sentToNavId: String? = null

        if (toggles.sendLpsPlanToFastlegeToggle && oppfolgingsplanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeId = isdialogmeldingConsumer.sendLpsPlanToFastlege(
                sykmeldtFnr,
                "<MOCK PDF CONTENT>".toByteArray(),
            )
        } else if (toggles.sendLpsPlanToNavToggle && oppfolgingsplanDTO.sendPlanToNav) {
            // TODO
            sentToNavId = null
        }
        return LpsOppfolgingsplanResponse(sykmeldtFnr = sykmeldtFnr, sentToFastlegeId = sentToFastlegeId, sentToNavId = sentToNavId)
    }
}

data class LpsOppfolgingsplanResponse(
    val sykmeldtFnr: String,
    val sentToFastlegeId: String?,
    val sentToNavId: String?,
) : Serializable
