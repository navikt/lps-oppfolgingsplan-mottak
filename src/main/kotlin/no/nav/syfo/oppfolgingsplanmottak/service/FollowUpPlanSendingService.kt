package no.nav.syfo.oppfolgingsplanmottak.service

import java.time.LocalDate
import java.util.*
import no.nav.syfo.altinnmottak.kafka.domain.KFollowUpPlan
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
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
    private val dokarkivClient: DokarkivClient,
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
        var pdf: ByteArray? = null

        if ((toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner) || (toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav)) {
            pdf = opPdfGenClient.getLpsPdf(followUpPlanDTO)
        }

        if (toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner) {
            if (pdf != null) {
                sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(
                    sykmeldtFnr,
                    pdf
                )
            }
            log.warn("Could not send LPS-plan to general practitioner because PDF is null")
        }

        if (toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav) {
            val needsHelpFromNav = followUpPlanDTO.needsHelpFromNav ?: false
            if (needsHelpFromNav) {
                sentToNavStatus = true
                val planToSendToNav = KFollowUpPlan(
                    uuid.toString(),
                    followUpPlanDTO.employeeIdentificationNumber,
                    employerOrgnr,
                    true,
                    LocalDate.now().toEpochDay().toInt(),
                )
                followupPlanProducer.sendFollowUpPlanToNav(planToSendToNav)
                if (pdf != null) {
                    dokarkivClient.journalforLps(followUpPlanDTO, employerOrgnr, pdf)
                } else {
                    log.warn("Could not send LPS-plan to NAV because PDF is null")
                }
            }
        }

        return FollowUpPlanResponse(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = sentToNavStatus,
            pdf = pdf
        )
    }
}
