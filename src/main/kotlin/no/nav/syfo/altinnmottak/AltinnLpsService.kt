package no.nav.syfo.altinnmottak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.op2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.helse.op2016.Skjemainnhold
import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.altinnmottak.database.setSentToFastlegeTrue
import no.nav.syfo.altinnmottak.database.setSentToNavTrue
import no.nav.syfo.altinnmottak.kafka.AltinnOppfolgingsplanProducer
import no.nav.syfo.altinnmottak.kafka.domain.KAltinnOppfolgingsplan
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.metric.COUNT_METRIKK_BISTAND_FRA_NAV_FALSE
import no.nav.syfo.application.metric.COUNT_METRIKK_BISTAND_FRA_NAV_TRUE
import no.nav.syfo.application.metric.COUNT_METRIKK_DELT_MED_FASTLEGE
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.util.xmlMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class AltinnLpsService(
    private val database: DatabaseInterface,
    private val navLpsProducer: AltinnOppfolgingsplanProducer,
    private val isdialogmeldingConsumer: IsdialogmeldingClient,
    private val dokarkivConsumer: DokarkivClient,
    private val sendToFastlegeRetryThreshold: Int,
) {
    private val log: Logger = LoggerFactory.getLogger(AltinnLpsService::class.qualifiedName)

    fun sendToFastlegeRetryThreshold() = sendToFastlegeRetryThreshold

    private fun xmlToSkjemainnhold(xml: String): Skjemainnhold = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(xml).skjemainnhold

    fun sendLpsPlanToNav(
        uuid: UUID,
        mostRecentFnr: String,
        orgnummer: String,
        hasBehovForBistand: Boolean,
    ) {
        val todayInEpoch = LocalDate.now().toEpochDay().toInt()
        val planToSendToNav =
            KAltinnOppfolgingsplan(
                uuid = uuid.toString(),
                fodselsnummer = mostRecentFnr,
                virksomhetsnummer = orgnummer,
                behovForBistandFraNav = hasBehovForBistand,
                opprettet = todayInEpoch,
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
