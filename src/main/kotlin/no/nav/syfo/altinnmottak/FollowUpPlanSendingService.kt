package no.nav.syfo.altinnmottak

import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import java.util.*

class FollowUpPlanSendingService(
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val altinnLpsService: AltinnLpsService,
    private val toggles: ToggleEnv,
) {
    suspend fun sendFollowUpPlan(
        followUpPlanDTO: FollowUpPlanDTO,
        uuid: UUID,
        employerOrgnr: String,
    ): FollowUpPlanResponse {
        val sykmeldtFnr = followUpPlanDTO.employeeIdentificationNumber

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null

        if (toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(sykmeldtFnr, "<MOCK PDF CONTENT>".toByteArray())
        }

        if (toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav) {
            altinnLpsService.sendLpsPlanToNav(
                uuid,
                followUpPlanDTO.employeeIdentificationNumber,
                employerOrgnr,
                followUpPlanDTO.needsHelpFromNav ?: false,
            )
            sentToNavStatus = true
        }

        return FollowUpPlanResponse(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = sentToNavStatus,
        )
    }
}
