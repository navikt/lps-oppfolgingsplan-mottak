package no.nav.syfo.oppfolgingsplanmottak.service

import java.time.LocalDate
import java.util.*
import no.nav.syfo.altinnmottak.kafka.domain.KFollowUpPlan
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer

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

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null

        if (toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(
                sykmeldtFnr,
                "<MOCK PDF CONTENT>".toByteArray()
            )
        }

        if (toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav) {
            val needsHelpFromNav = followUpPlanDTO.needsHelpFromNav ?: false
            sentToNavStatus = true
            if (needsHelpFromNav) {
                val planToSendToNav = KFollowUpPlan(
                    uuid.toString(),
                    followUpPlanDTO.employeeIdentificationNumber,
                    employerOrgnr,
                    needsHelpFromNav,
                    LocalDate.now().toEpochDay().toInt(),
                )
                followupPlanProducer.sendFollowUpPlanToNav(planToSendToNav)
            }
        }

        return FollowUpPlanResponse(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = sentToNavStatus,
        )
    }
}
