package no.nav.syfo.altinnmottak

import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import org.slf4j.LoggerFactory
import java.util.UUID

class FollowUpPlanSendingService(
    private val opPdfGenConsumer: OpPdfGenClient,
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val dokarkivConsumer: DokarkivClient,
    private val toggles: ToggleEnv,
) {
    private val log = LoggerFactory.getLogger(FollowUpPlanSendingService::class.qualifiedName)

    suspend fun sendFollowUpPlan(
        oppfolgingsplanDTO: FollowUpPlanDTO,
        uuid: UUID,
    ): FollowUpPlanResponse {
        val sykmeldtFnr = oppfolgingsplanDTO.employeeIdentificationNumber

        var sentToFastlegeStatus: Boolean? = null
        var sentToNavStatus: Boolean? = null

        if (toggles.sendLpsPlanToFastlegeToggle && oppfolgingsplanDTO.sendPlanToGeneralPractitioner) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeStatus =
                isdialogmeldingConsumer.sendLpsPlanToFastlege(
                    sykmeldtFnr,
                    "<MOCK PDF CONTENT>".toByteArray(),
                )
        } else if (toggles.sendLpsPlanToNavToggle && oppfolgingsplanDTO.sendPlanToNav) {
            // TODO: isPersonoppgaveClient.sendLpsPlanToNav()
            sentToNavStatus = false
        }
        return FollowUpPlanResponse(uuid = uuid.toString(), sentToGeneralPractitionerStatus = sentToFastlegeStatus, sentToNavStatus = sentToNavStatus)
    }

    suspend fun sendLpsPlanDummy() {
        isdialogmeldingConsumer.sendLpsPlanToFastlege(
            "12121212121",
            "<MOCK PDF CONTENT>".toByteArray(),
        )
    }
}
