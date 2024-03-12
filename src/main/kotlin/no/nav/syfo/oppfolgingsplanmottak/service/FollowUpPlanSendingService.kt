package no.nav.syfo.oppfolgingsplanmottak.service

import java.time.LocalDate
import java.util.*
import no.nav.syfo.altinnmottak.kafka.domain.KFollowUpPlan
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import org.slf4j.LoggerFactory

class FollowUpPlanSendingService(
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val followupPlanProducer: FollowUpPlanProducer,
    private val opPdfGenClient: OpPdfGenClient,
    private val toggles: ToggleEnv,
) {
    val log = LoggerFactory.getLogger(FollowUpPlanSendingService::class.qualifiedName)
    suspend fun sendFollowUpPlan(
        followUpPlanDTO: FollowUpPlanDTO,
        uuid: UUID,
        employerOrgnr: String,
    ): FollowUpPlanResponse {
        val sykmeldtFnr = followUpPlanDTO.employeeIdentificationNumber

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null


        // TODO: does isdialogmelding journalorer lps plans?
        // TODO: if it does, then journalfor  unsent
        if (toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner) {
            val pdf = opPdfGenClient.getLpsPdf(followUpPlanDTO)
            log.warn("QWQW: pdf ${pdf}")
            if (pdf != null){
                sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(
                    sykmeldtFnr,
                    pdf
                )
                // todo: Journalfor
            }
        }

        if (toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav) {
            val needsHelpFromNav = followUpPlanDTO.needsHelpFromNav ?: false
            sentToNavStatus = true
            if (needsHelpFromNav) {
                val planToSendToNav = KFollowUpPlan(
                    uuid.toString(),
                    followUpPlanDTO.employeeIdentificationNumber,
                    employerOrgnr,
                    true,
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
