package no.nav.syfo.oppfolgingsplanmottak.service

import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import java.time.LocalDate
import java.util.*
import no.nav.syfo.oppfolgingsplanmottak.kafka.domain.KFollowUpPlan
import org.slf4j.LoggerFactory

class FollowUpPlanSendingService(
    private val isdialogmeldingClient: IsdialogmeldingClient,
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
        var pdf: ByteArray? = null
        val shouldSendToNav = shouldSendToNav(followUpPlanDTO)
        val shouldSendToGeneralPractitioner = shouldSendToGeneralPractitioner(toggles, followUpPlanDTO)

        if (shouldSendToGeneralPractitioner || shouldSendToNav) {
            pdf = opPdfGenClient.getLpsPdf(followUpPlanDTO)
        }

        val sentToFastlegeStatus: Boolean =
            shouldSendToGeneralPractitioner && run {
                if (pdf != null) {
                    isdialogmeldingClient.sendLpsPlanToGeneralPractitioner(
                        sykmeldtFnr,
                        pdf
                    )
                    true
                } else {
                    false
                }
            }

        if (shouldSendToNav) {
            val planToSendToNav = KFollowUpPlan(
                uuid.toString(),
                followUpPlanDTO.employeeIdentificationNumber,
                employerOrgnr,
                true,
                LocalDate.now().toEpochDay().toInt(),
            )
            followupPlanProducer.createFollowUpPlanTaskInModia(planToSendToNav)
        }

        if (pdf != null && followUpPlanDTO.sendPlanToNav) {
            dokarkivClient.journalforLps(followUpPlanDTO, employerOrgnr, pdf, uuid)
        } else {
            log.warn("Could not send LPS-plan with uuid $uuid to NAV because PDF is null")
        }

        return FollowUpPlan(
            uuid = uuid.toString(),
            isSentToGeneralPractitionerStatus = sentToFastlegeStatus,
            isSentToNavStatus = followUpPlanDTO.sendPlanToNav,
            pdf = pdf
        )
    }

    private fun shouldSendToNav(followUpPlanDTO: FollowUpPlanDTO): Boolean {
        return followUpPlanDTO.sendPlanToNav && followUpPlanDTO.needsHelpFromNav == true
    }

    private fun shouldSendToGeneralPractitioner(toggles: ToggleEnv, followUpPlanDTO: FollowUpPlanDTO): Boolean {
        return toggles.sendLpsPlanToFastlegeToggle && followUpPlanDTO.sendPlanToGeneralPractitioner
    }
}
