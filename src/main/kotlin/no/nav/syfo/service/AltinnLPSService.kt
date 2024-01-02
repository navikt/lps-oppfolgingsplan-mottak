package no.nav.syfo.service

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.op2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.syfo.consumer.dokarkiv.DokarkivConsumer
import no.nav.syfo.consumer.isdialogmelding.IsdialogmeldingConsumer
import no.nav.syfo.consumer.oppdfgen.OpPdfGenConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.db.*
import no.nav.syfo.db.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.kafka.KOppfolgingsplanLPS
import no.nav.syfo.kafka.producers.NavLpsProducer
import no.nav.syfo.metrics.COUNT_METRIKK_BISTAND_FRA_NAV_FALSE
import no.nav.syfo.metrics.COUNT_METRIKK_BISTAND_FRA_NAV_TRUE
import no.nav.syfo.metrics.COUNT_METRIKK_DELT_MED_FASTLEGE
import no.nav.syfo.metrics.COUNT_METRIKK_PROSSESERING_VELLYKKET
import no.nav.syfo.service.domain.isBehovForBistandFraNAV
import no.nav.syfo.util.mapFormdataToFagmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Suppress("LongParameterList")
class AltinnLPSService(
    private val pdlConsumer: PdlConsumer,
    private val opPdfGenConsumer: OpPdfGenConsumer,
    private val database: DatabaseInterface,
    private val navLpsProducer: NavLpsProducer,
    private val isdialogmeldingConsumer: IsdialogmeldingConsumer,
    private val dokarkivConsumer: DokarkivConsumer,
    private val sendToGpRetryThreshold: Int,
) {
    private val log: Logger = LoggerFactory.getLogger(AltinnLPSService::class.qualifiedName)

    fun persistLpsPlan(
        archiveReference: String,
        payload: String,
    ): UUID {
        val oppfolgingsplan = xmlToOppfolgingsplan(payload)
        val arbeidstakerFnr = oppfolgingsplan.skjemainnhold.sykmeldtArbeidstaker.fnr
        val orgnummer = oppfolgingsplan.skjemainnhold.arbeidsgiver.orgnr
        val shouldSendToNav = oppfolgingsplan.skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTiNav
        val shouldSendToGP = oppfolgingsplan.skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege
        val now = LocalDateTime.now()

        val lpsPlanToSave = AltinnLpsOppfolgingsplan(
            archiveReference,
            UUID.randomUUID(),
            arbeidstakerFnr,
            null,
            orgnummer,
            null,
            payload,
            shouldSendToNav,
            shouldSendToGP,
            false, sentToGp = false,
            0,
            null,
            now,
            now,
            now
        )
        database.storeAltinnLps(lpsPlanToSave)
        return lpsPlanToSave.uuid
    }

    fun processLpsPlan(lpsUuid: UUID) {
        val altinnLps = database.getLpsByUuid(lpsUuid)
        val lpsFnr = altinnLps.lpsFnr

        val mostRecentFnr = pdlConsumer.mostRecentFnr(lpsFnr)
        if (mostRecentFnr == null) {
            log.info("[ALTINN-KANAL-2]: Unable to determine most recent FNR for Altinn LPS" +
                    "with AR: ${altinnLps.archiveReference}")
            return
        }

        database.storeFnr(lpsUuid, mostRecentFnr)

        val skjemainnhold = xmlToSkjemainnhold(altinnLps.xml)
        val lpsPdfModel = mapFormdataToFagmelding(
            mostRecentFnr,
            skjemainnhold,
        )
        val pdf = opPdfGenConsumer.generatedPdfResponse(lpsPdfModel)
        if (pdf == null) {
            log.info("[ALTINN-KANAL-2]: Unable to generate PDF for Altinn-LPS with AR: ${altinnLps.archiveReference}")
            return
        }

        database.storePdf(lpsUuid, pdf)

        val shouldSendToNav = skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTiNav
        if (shouldSendToNav) {
            sendLpsPlanToNav(
                lpsUuid,
                mostRecentFnr,
                skjemainnhold.arbeidsgiver.orgnr,
                lpsPdfModel.oppfolgingsplan.isBehovForBistandFraNAV(),
            )
        }

        val shouldBeSentToGP = skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege
        if (shouldBeSentToGP) {
            sendLpsPlanToGeneralPractitioner(
                lpsUuid,
                lpsFnr,
                pdf,
            )
        }
    }

    fun retryStoreFnr(
        uuid: UUID,
        lpsFnr: String,
    ): Boolean {
        val mostRecentFnr = pdlConsumer.mostRecentFnr(lpsFnr)
        return mostRecentFnr?.let {
            database.storeFnr(uuid, mostRecentFnr)
            log.info("Successfully stored fnr on retry attempt for altinn-lps with UUID: $uuid")
            true
        } ?: false
    }

    fun retryStorePdf(
        uuid: UUID,
        fnr: String,
        xml: String,
    ): Boolean {
        val skjemainnhold = xmlToSkjemainnhold(xml)
        val shouldNotBeSentToGP = !skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege
        val lpsPdfModel = mapFormdataToFagmelding(
            fnr,
            skjemainnhold,
        )
        val pdf = opPdfGenConsumer.generatedPdfResponse(lpsPdfModel)
        return pdf?.let {
            database.storePdf(uuid, pdf)
            log.info("Successfully stored PDF on retry attempt for altinn-lps with UUID: $uuid")
            if (shouldNotBeSentToGP) {
                COUNT_METRIKK_PROSSESERING_VELLYKKET.increment()
            }
            true
        } ?: false
    }

    fun sendToGpRetryThreshold() = sendToGpRetryThreshold

    fun xmlToOppfolgingsplan(xml: String) = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(xml)

    fun xmlToSkjemainnhold(xml: String) = xmlToOppfolgingsplan(xml).skjemainnhold

    fun sendLpsPlanToNav(
        uuid: UUID,
        mostRecentFnr: String,
        orgnummer: String,
        hasBehovForBistand: Boolean,
    ) {
        val todayInEpoch = LocalDate.now().toEpochDay().toInt()
        val planToSendToNav = KOppfolgingsplanLPS(
            uuid.toString(),
            mostRecentFnr,
            orgnummer,
            hasBehovForBistand,
            todayInEpoch
        )
        navLpsProducer.sendAltinnLpsToNav(planToSendToNav)
        database.setSentToNavTrue(uuid)
        if (hasBehovForBistand) {
            COUNT_METRIKK_BISTAND_FRA_NAV_TRUE.increment()
        } else {
            COUNT_METRIKK_BISTAND_FRA_NAV_FALSE.increment()
        }
    }

    fun sendLpsPlanToGeneralPractitioner(
        uuid: UUID,
        lpsFnr: String,
        pdf: ByteArray,
    ): Boolean {
        val success = isdialogmeldingConsumer.sendPlanToGeneralPractitioner(lpsFnr, pdf)
        if (success) {
            database.setSentToGpTrue(uuid)
            COUNT_METRIKK_DELT_MED_FASTLEGE.increment()
        }
        return success
    }

    fun sendLpsPlanToGosys(lps: AltinnLpsOppfolgingsplan): String {
        val skjemainnhold = xmlToSkjemainnhold(lps.xml)
        val virksomhetsnavn = skjemainnhold.arbeidsgiver.orgnavn

        return dokarkivConsumer.journalforAltinnLps(lps, virksomhetsnavn)
    }
}
