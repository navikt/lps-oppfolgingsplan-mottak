package no.nav.syfo.scheduling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.op2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.db.getLpsNotYetSentToGp
import no.nav.syfo.db.getLpsNotYetSentToNav
import no.nav.syfo.db.updateSendToGpRetryCount
import no.nav.syfo.metrics.COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING
import no.nav.syfo.metrics.COUNT_METRIKK_PROSSESERING_VELLYKKET
import no.nav.syfo.service.AltinnLPSService
import no.nav.syfo.service.domain.isBehovForBistandFraNAV
import no.nav.syfo.service.xmlMapper
import no.nav.syfo.util.mapFormdataToFagmelding
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class AltinnLpsRetryForwardLpsJob: Job {
    private val log = LoggerFactory.getLogger(AltinnLpsRetryForwardLpsJob::class.qualifiedName)
    private val jobName = "RETRY_FORWARD_LPS_JOB"
    private val jobLogPrefix = "[$jobName]:"

    override fun execute(context: JobExecutionContext) {
        logInfo("Starting job $jobName")
        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val altinnLpsService = jobDataMap[LPS_SERVICE_SHORTNAME] as AltinnLPSService
        retryForwardAltinnLps(database, altinnLpsService)
        logInfo("$jobName job successfully finished")
    }

    private fun retryForwardAltinnLps(
        database: DatabaseInterface,
        altinnLpsService: AltinnLPSService,
    ) {
        val sendToGpAttemptThreshold = altinnLpsService.sendToGpRetryThreshold()
        val lpsNotYetSentToNav = database.getLpsNotYetSentToNav()
        val lpsNotYetSentToGp = database.getLpsNotYetSentToGp(sendToGpAttemptThreshold)
        forwardUnsentLpsToGp(database, altinnLpsService, lpsNotYetSentToGp)
        forwardUnsentLpsToNav(altinnLpsService, lpsNotYetSentToNav)
    }

    private fun forwardUnsentLpsToGp(
            database: DatabaseInterface,
            altinnLpsService: AltinnLPSService,
            lpsNotYetSentToGp: List<AltinnLpsOppfolgingsplan>
    ) {
        lpsNotYetSentToGp.forEach { lps ->
            val success = altinnLpsService.sendLpsPlanToGeneralPractitioner(
                    lps.uuid,
                    lps.lpsFnr,
                    lps.pdf!!
            )
            if (!success) {
                database.updateSendToGpRetryCount(lps.uuid, lps.sendToGpRetryCount)
            } else {
                COUNT_METRIKK_PROSSESERING_VELLYKKET.increment()
                COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING.increment()
            }
        }
    }

    private fun forwardUnsentLpsToNav(
            altinnLpsService: AltinnLPSService,
            lpsNotYetSentToNav: List<AltinnLpsOppfolgingsplan>
    ) {
        lpsNotYetSentToNav.forEach { lps ->
            val skjemainnhold = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(lps.xml).skjemainnhold
            val oppfolgingsplan = mapFormdataToFagmelding(
                    lps.fnr!!,
                    skjemainnhold,
            ).oppfolgingsplan
            val harBehovForBistand = oppfolgingsplan.isBehovForBistandFraNAV()
            altinnLpsService.sendLpsPlanToNav(
                    lps.uuid,
                    lps.fnr,
                    lps.orgnummer,
                    harBehovForBistand,
            )
        }
    }

    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")
}
