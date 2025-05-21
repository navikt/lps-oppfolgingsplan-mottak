package no.nav.syfo.altinnmottak

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.op2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.helse.op2016.Skjemainnhold
import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.altinnmottak.database.getAltinnLpsOppfolgingsplanByUuid
import no.nav.syfo.altinnmottak.database.setSentToFastlegeTrue
import no.nav.syfo.altinnmottak.database.setSentToNavTrue
import no.nav.syfo.altinnmottak.database.storeAltinnLpsOppfolgingsplan
import no.nav.syfo.altinnmottak.database.storeFnr
import no.nav.syfo.altinnmottak.database.storePdf
import no.nav.syfo.altinnmottak.domain.isBehovForBistandFraNAV
import no.nav.syfo.altinnmottak.kafka.AltinnOppfolgingsplanProducer
import no.nav.syfo.altinnmottak.kafka.domain.KAltinnOppfolgingsplan
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.application.metric.COUNT_METRIKK_BISTAND_FRA_NAV_FALSE
import no.nav.syfo.application.metric.COUNT_METRIKK_BISTAND_FRA_NAV_TRUE
import no.nav.syfo.application.metric.COUNT_METRIKK_DELT_MED_FASTLEGE
import no.nav.syfo.application.metric.COUNT_METRIKK_PROSSESERING_VELLYKKET
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.util.mapFormdataToFagmelding
import no.nav.syfo.util.xmlMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
class AltinnLpsService(
    private val pdlConsumer: PdlClient,
    private val opPdfGenConsumer: OpPdfGenClient,
    private val database: DatabaseInterface,
    private val navLpsProducer: AltinnOppfolgingsplanProducer,
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val dokarkivConsumer: DokarkivClient,
    private val sendToFastlegeRetryThreshold: Int,
    private val toggles: ToggleEnv,
) {
    private val log: Logger = LoggerFactory.getLogger(AltinnLpsService::class.qualifiedName)

    fun persistLpsPlan(
        archiveReference: String?,
        payload: String,
    ): UUID {
        val oppfolgingsplan = xmlToOppfolgingsplan(payload)
        val arbeidstakerFnr = oppfolgingsplan.skjemainnhold.sykmeldtArbeidstaker.fnr
        val orgnummer = oppfolgingsplan.skjemainnhold.arbeidsgiver.orgnr
        val shouldSendToNav = oppfolgingsplan.skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTiNav ?: false
        val shouldSendToFastlege =
            oppfolgingsplan.skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege ?: false
        val now = LocalDateTime.now()

        val lpsPlanToSave = AltinnLpsOppfolgingsplan(
            uuid = UUID.randomUUID(),
            lpsFnr = arbeidstakerFnr,
            fnr = null,
            orgnummer = orgnummer,
            pdf = null,
            xml = payload,
            shouldSendToNav = shouldSendToNav,
            shouldSendToFastlege = shouldSendToFastlege,
            sentToNav = false,
            sentToFastlege = false,
            sendToFastlegeRetryCount = 0,
            journalpostId = null,
            archiveReference = archiveReference,
            created = now,
            lastChanged = now,
        )
        database.storeAltinnLpsOppfolgingsplan(lpsPlanToSave)
        return lpsPlanToSave.uuid
    }

    suspend fun processLpsPlan(lpsUuid: UUID) {
        val altinnLps = database.getAltinnLpsOppfolgingsplanByUuid(lpsUuid)
        val lpsFnr = altinnLps.lpsFnr

        val mostRecentFnr =
            pdlConsumer.mostRecentFnr(lpsFnr)

        if (mostRecentFnr == null) {
            log.warn(
                "[ALTINN-KANAL-2]: Unable to determine most recent FNR for Altinn LPS " +
                        "with UUID ${altinnLps.uuid} and archive reference: ${altinnLps.archiveReference}"
            )
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
            log.warn(
                "[ALTINN-KANAL-2]: Unable to generate PDF for Altinn-LPS with " +
                    "UUID ${altinnLps.uuid} and archive reference: ${altinnLps.archiveReference}"
            )
            return
        }

        database.storePdf(lpsUuid, pdf)

        val shouldSendToNav = skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTiNav
        if (shouldSendToNav && toggles.sendAltinnLpsPlanToNavToggle) {
            sendLpsPlanToNav(
                lpsUuid,
                mostRecentFnr,
                skjemainnhold.arbeidsgiver.orgnr,
                lpsPdfModel.oppfolgingsplan.isBehovForBistandFraNAV(),
            )
        }

        val shouldBeSentToGP = skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege
        if (shouldBeSentToGP && toggles.sendAltinnLpsPlanToFastlegeToggle) {
            sendLpsPlanToGeneralPractitioner(
                lpsUuid,
                lpsFnr,
                pdf,
            )
        }
    }

    suspend fun retryStoreFnr(
        uuid: UUID,
        lpsFnr: String,
    ): Boolean {
        return try {
            val mostRecentFnr = pdlConsumer.mostRecentFnr(lpsFnr)

            if (mostRecentFnr == null) {
                log.error("Unable to determine most recent fnr on retry attempt for altinn-lps with UUID: $uuid")
                false
            } else {
                database.storeFnr(uuid, mostRecentFnr)
                log.info("Successfully stored fnr on retry attempt for altinn-lps with UUID: $uuid")
                true
            }
        } catch (e: RuntimeException) {
            log.error("Error encountered while retrying fnr fetch", e)
            false
        }
    }

    suspend fun retryStorePdf(
        uuid: UUID,
        fnr: String,
        xml: String,
    ): Boolean {
        return try {
            val skjemainnhold = xmlToSkjemainnhold(xml)
            val shouldNotBeSentToGP = !skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege
            val lpsPdfModel = mapFormdataToFagmelding(
                fnr,
                skjemainnhold,
            )
            val pdf = opPdfGenConsumer.generatedPdfResponse(lpsPdfModel)
            pdf?.let {
                database.storePdf(uuid, pdf)
                log.info("Successfully stored PDF on retry attempt for altinn-lps with UUID: $uuid")
                if (shouldNotBeSentToGP) {
                    COUNT_METRIKK_PROSSESERING_VELLYKKET.increment()
                }
                true
            } ?: false
        } catch (e: RuntimeException) {
            log.error("Error encountered while retrying PDF-generation", e)
            false
        }
    }

    fun sendToFastlegeRetryThreshold() = sendToFastlegeRetryThreshold

    private fun xmlToOppfolgingsplan(xml: String) = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(xml)

    private fun xmlToSkjemainnhold(xml: String): Skjemainnhold = xmlToOppfolgingsplan(xml).skjemainnhold

    fun sendLpsPlanToNav(
        uuid: UUID,
        mostRecentFnr: String,
        orgnummer: String,
        hasBehovForBistand: Boolean,
    ) {
        val todayInEpoch = LocalDate.now().toEpochDay().toInt()
        val planToSendToNav = KAltinnOppfolgingsplan(
            uuid.toString(),
            mostRecentFnr,
            orgnummer,
            hasBehovForBistand,
            todayInEpoch,
        )
        navLpsProducer.sendAltinnLpsToNav(planToSendToNav)
        database.setSentToNavTrue(uuid)
        if (hasBehovForBistand) {
            COUNT_METRIKK_BISTAND_FRA_NAV_TRUE.increment()
        } else {
            COUNT_METRIKK_BISTAND_FRA_NAV_FALSE.increment()
        }
    }

    suspend fun sendLpsPlanToGeneralPractitioner(
        uuid: UUID,
        lpsFnr: String,
        pdf: ByteArray,
    ): Boolean {
        val success = isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(lpsFnr, pdf)
        if (success) {
            database.setSentToFastlegeTrue(uuid)
            COUNT_METRIKK_DELT_MED_FASTLEGE.increment()
        }
        return success
    }

    suspend fun sendLpsPlanToGosys(lps: AltinnLpsOppfolgingsplan): String {
        log.info("Sending plan to dokarkiv with uuid ${lps.uuid}")
        val skjemainnhold = xmlToSkjemainnhold(lps.xml)
        val virksomhetsnavn = skjemainnhold.arbeidsgiver.orgnavn

        return dokarkivConsumer.journalforAltinnLps(lps, virksomhetsnavn)
    }
}
