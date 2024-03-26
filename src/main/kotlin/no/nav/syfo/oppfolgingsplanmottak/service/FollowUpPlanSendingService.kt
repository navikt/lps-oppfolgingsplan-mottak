package no.nav.syfo.oppfolgingsplanmottak.service

import java.time.LocalDate
import java.util.*
import no.nav.syfo.altinnmottak.kafka.domain.KFollowUpPlan
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
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
    ): FollowUpPlan {
        val sykmeldtFnr = followUpPlanDTO.employeeIdentificationNumber

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null
        var pdf: ByteArray? = null
        val shouldSendToNav = shouldSendToNav(toggles, followUpPlanDTO)
        val shouldSendToGeneralPractitioner = shouldSendToGeneralPractitioner(toggles, followUpPlanDTO)

        if (shouldSendToGeneralPractitioner || shouldSendToNav) {
            pdf = opPdfGenClient.getLpsPdf(followUpPlanDTO)
        }

        if (shouldSendToGeneralPractitioner) {
            if (pdf != null) {
                sentToFastlegeStatus = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(
                    sykmeldtFnr,
                    pdf
                )
            } else {
                log.warn("Could not send LPS-plan to general practitioner because PDF is null")
            }
        }

        if (shouldSendToNav) {
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
                    dokarkivClient.journalforLps(followUpPlanDTO, employerOrgnr, pdf, uuid)
                } else {
                    log.warn("Could not send LPS-plan to NAV because PDF is null")
                }
            }
        }

        return FollowUpPlan(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = sentToNavStatus,
            pdf = pdf
        )
    }

    private fun shouldSendToNav(toggles: ToggleEnv, followUpPlanDTO: FollowUpPlanDTO): Boolean {
        return toggles.sendLpsPlanToNavToggle && followUpPlanDTO.sendPlanToNav
    }

    private fun shouldSendToGeneralPractitioner(toggles: ToggleEnv, followUpPlanDTO: FollowUpPlanDTO): Boolean {
        return toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner
    }
}
