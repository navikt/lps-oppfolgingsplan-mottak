package no.nav.syfo.service

import no.nav.syfo.api.lps.Mottaker
import no.nav.syfo.api.lps.OppfolgingsplanDTO
import no.nav.syfo.consumer.dokarkiv.DokarkivConsumer
import no.nav.syfo.consumer.isdialogmelding.IsdialogmeldingConsumer
import no.nav.syfo.consumer.oppdfgen.OpPdfGenConsumer
import no.nav.syfo.environment.ToggleEnv
import no.nav.syfo.kafka.producers.NavLpsProducer
import java.io.Serializable

class LpsOppfolgingsplanSendingService(
    private val opPdfGenConsumer: OpPdfGenConsumer,
    private val navLpsProducer: NavLpsProducer,
    private val isdialogmeldingConsumer: IsdialogmeldingConsumer,
    private val dokarkivConsumer: DokarkivConsumer,
    private val toggles: ToggleEnv,
) {
    suspend fun sendLpsPlan(oppfolgingsplanDTO: OppfolgingsplanDTO): LpsOppfolgingsplan {
        val sykmeldtFnr = oppfolgingsplanDTO.oppfolgingsplanMeta.sykmeldtFnr
        val mottaker = oppfolgingsplanDTO.oppfolgingsplanMeta.mottaker
        // TODO:       val pdf = opPdfGenConsumer.generatedPdfResponse("new model")

        var sentToFastlegeId: String? = null
        var sentToNavId: String? = null

        if (toggles.sendLpsPlanToFastlegeToggle && shouldBeSentToFastlege(mottaker)) {
            // TODO: send actual PDF when data model and pdfgen are updated
            sentToFastlegeId = isdialogmeldingConsumer.sendLpsPlanToFastlege(
                sykmeldtFnr,
                ByteArray(1),
            )
        } else if (toggles.sendLpsPlanToNavToggle && shouldBeSentToNav(mottaker)) {
            // TODO
            sentToNavId = null
        }
        return LpsOppfolgingsplan(sykmeldtFnr = sykmeldtFnr, sentToFastlegeId = sentToFastlegeId, sentToNavId = sentToNavId)
    }

    private fun shouldBeSentToFastlege(mottaker: Mottaker): Boolean {
        return mottaker == Mottaker.FASTLEGE || mottaker == Mottaker.NAVOGFASTLEGE
    }

    private fun shouldBeSentToNav(mottaker: Mottaker): Boolean {
        return mottaker == Mottaker.NAV || mottaker == Mottaker.NAVOGFASTLEGE
    }
}

data class LpsOppfolgingsplan(
    val sykmeldtFnr: String,
    val sentToFastlegeId: String?,
    val sentToNavId: String?,
) : Serializable
