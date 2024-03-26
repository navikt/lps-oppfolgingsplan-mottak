package no.nav.syfo.oppfolgingsplanmottak.service

import no.nav.syfo.altinnmottak.kafka.domain.KFollowUpPlan
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import java.time.LocalDate
import java.util.*

class FollowUpPlanSendingService(
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val followupPlanProducer: FollowUpPlanProducer,
    private val toggles: ToggleEnv,
) {
    suspend fun sendFollowUpPlan(
        followUpPlanDTO: FollowUpPlanDTO,
        uuid: UUID,
        employerOrgnr: String,
    ): FollowUpPlanResponse {
        val sykmeldtFnr = followUpPlanDTO.employeeIdentificationNumber

        val sentToFastlegeStatus: Boolean =
            toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner && run {
                // TODO: send actual PDF when data model and pdfgen are updated
                isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(
                    sykmeldtFnr,
                    "<MOCK PDF CONTENT>".toByteArray()
                )
                true
            }

        if (followUpPlanDTO.sendPlanToNav && followUpPlanDTO.needsHelpFromNav == true) {
            val planToSendToNav = KFollowUpPlan(
                uuid.toString(),
                followUpPlanDTO.employeeIdentificationNumber,
                employerOrgnr,
                true,
                LocalDate.now().toEpochDay().toInt(),
            )
            followupPlanProducer.createFollowUpPlanTaskInModia(planToSendToNav)
        }

        return FollowUpPlanResponse(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = followUpPlanDTO.sendPlanToNav,
        )
    }
}
