package no.nav.syfo.altinnmottak

import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import java.util.*

class FollowUpPlanSendingService(
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val toggles: ToggleEnv,
) {
    suspend fun sendFollowUpPlan(
        oppfolgingsplanDTO: FollowUpPlanDTO,
        uuid: UUID,
    ): FollowUpPlanResponse {
        val sykmeldtFnr = oppfolgingsplanDTO.employeeIdentificationNumber

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null

        if (toggles.sendLpsPlanToFastlegeToggle && oppfolgingsplanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToFastlege(sykmeldtFnr, "<MOCK PDF CONTENT>".toByteArray())
        } else if (toggles.sendLpsPlanToNavToggle && oppfolgingsplanDTO.sendPlanToNav) {
            sentToNavStatus = null
        }
        return FollowUpPlanResponse(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = sentToNavStatus,
        )
    }
}
