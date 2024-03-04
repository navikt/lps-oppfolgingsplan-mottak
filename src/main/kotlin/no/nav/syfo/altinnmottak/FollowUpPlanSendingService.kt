package no.nav.syfo.altinnmottak

import java.util.*
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FollowUpPlanSendingService(
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val altinnLpsService: AltinnLpsService,
    private val toggles: ToggleEnv,
) {
    private val log: Logger = LoggerFactory.getLogger(FollowUpPlanSendingService::class.qualifiedName)
    suspend fun sendFollowUpPlan(
        followUpPlanDTO: FollowUpPlanDTO,
        uuid: UUID,
        employerOrgnr: String,
    ): FollowUpPlanResponse {
        val sykmeldtFnr = followUpPlanDTO.employeeIdentificationNumber

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null
        log.warn("qwqw sendLpsPlanToNavToggle: ${toggles.sendLpsPlanToNavToggle}")
        log.warn("qwqw followUpPlanDTO.sendPlanToNav: ${followUpPlanDTO.sendPlanToNav}")

        if (toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(sykmeldtFnr, "<MOCK PDF CONTENT>".toByteArray())
        } else if (toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav) {
            log.warn("qwqw sending ${followUpPlanDTO.sendPlanToNav}")
            altinnLpsService.sendLpsPlanToNav(
                uuid,
                followUpPlanDTO.employeeIdentificationNumber,
                employerOrgnr,
                followUpPlanDTO.needsHelpFromNav ?: false,
            )
            sentToNavStatus = true
            log.warn("qwqw sent: $sentToNavStatus")
        }
        return FollowUpPlanResponse(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = sentToNavStatus,
        )
    }
}
