package no.nav.syfo.scheduling

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.op2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.syfo.db.*
import no.nav.syfo.environment.ToggleEnv
import no.nav.syfo.metrics.COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING
import no.nav.syfo.metrics.COUNT_METRIKK_PROSSESERING_VELLYKKET
import no.nav.syfo.service.AltinnLpsService
import no.nav.syfo.service.domain.isBehovForBistandFraNAV
import no.nav.syfo.service.xmlMapper
import no.nav.syfo.util.LeaderElection
import no.nav.syfo.util.mapFormdataToFagmelding
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class AltinnLpsRetryForwardLpsJob: Job {
    private val log = LoggerFactory.getLogger(AltinnLpsRetryForwardLpsJob::class.qualifiedName)
    private val jobName = "FORWARD_LPS_JOB"
    private val jobLogPrefix = "[$jobName]:"

    override fun execute(context: JobExecutionContext) {
        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val altinnLpsService = jobDataMap[LPS_SERVICE_SHORTNAME] as AltinnLpsService
        val leaderElection = jobDataMap[LEADER_ELECTION_SHORTNAME] as LeaderElection
        val toggles = jobDataMap[TOGGLES_SHORTNAME] as ToggleEnv
        if (leaderElection.thisPodIsLeader()) {
            logInfo("Starting job $jobName")
            runBlocking {
                retryForwardAltinnLps(database, altinnLpsService, toggles)
            }
            logInfo("$jobName job successfully finished")
        }
    }

    private suspend fun retryForwardAltinnLps(
            database: DatabaseInterface,
            altinnLpsService: AltinnLpsService,
            toggles: ToggleEnv,
    ) {
        if (toggles.sendAltinnLpsPlanToNavToggle) {
            forwardUnsentLpsToNav(database, altinnLpsService)
        }
        if (toggles.sendAltinnLpsPlanToFastlegeToggle) {
            forwardUnsentLpsToFastlege(database, altinnLpsService)
        }
        if (toggles.journalforAltinnLpsPlanToggle) {
            forwardUnsentLpsToDokarkiv(database, altinnLpsService)
        }
    }

    private fun forwardUnsentLpsToNav(
            database: DatabaseInterface,
            altinnLpsService: AltinnLpsService,
    ) {
        val altinnLpsOppfolgingsplanNotYetSentToNav = database.getAltinnLpsOppfolgingsplanNotYetSentToNav()
        altinnLpsOppfolgingsplanNotYetSentToNav.forEach { lps ->
            val skjemainnhold = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(lps.xml).skjemainnhold
            val oppfolgingsplan = mapFormdataToFagmelding(
                    lps.fnr!!,
                    skjemainnhold,
            ).oppfolgingsplan
            val harBehovForBistand = oppfolgingsplan.isBehovForBistandFraNAV()
            try {
                altinnLpsService.sendLpsPlanToNav(
                    lps.uuid,
                    lps.fnr,
                    lps.orgnummer,
                    harBehovForBistand,
                )
            } catch (e: RuntimeException) {
                log.error("Could not forward altinn-lps with uuid ${lps.uuid} to NAV", e)
            }
        }
    }

    private suspend fun forwardUnsentLpsToFastlege(
            database: DatabaseInterface,
            altinnLpsService: AltinnLpsService,
    ) {
        val retryThreshold = altinnLpsService.sendToFastlegeRetryThreshold()
        val altinnLpsOppfolgingsplanNotYetSentToFastlege =
                database.getAltinnLpsOppfolgingsplanNotYetSentToFastlege(retryThreshold)
        altinnLpsOppfolgingsplanNotYetSentToFastlege.forEach { lps ->
            val success = try {
                altinnLpsService.sendLpsPlanToFastlege(
                    lps.uuid,
                    lps.lpsFnr,
                    lps.pdf!!
                )
            } catch (e: RuntimeException) {
                log.error("Could not forward altinn-lps with uuid ${lps.uuid} to fastlege", e)
                false
            }
            if (!success) {
                database.updateSendToFastlegeRetryCount(lps.uuid, lps.sendToFastlegeRetryCount)
            } else {
                COUNT_METRIKK_PROSSESERING_VELLYKKET.increment()
                COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING.increment()
            }
        }
    }

    private suspend fun forwardUnsentLpsToDokarkiv(
            database: DatabaseInterface,
            altinnLpsService: AltinnLpsService
    ) {
        val altinnLpsOppfolgingsplanNotYetSentToDokarkiv = database.getAltinnLpsOppfolgingsplanNotYetSentToDokarkiv()
        altinnLpsOppfolgingsplanNotYetSentToDokarkiv.forEach { lps ->
            val journalpostId = try {
                 altinnLpsService.sendLpsPlanToGosys(lps)
            } catch (e: RuntimeException) {
                log.error("Could not forward altinn-lps with uuid ${lps.uuid} to DokArkiv", e)
                null
            }
            journalpostId?.let { database.updateJournalpostId(lps.uuid, journalpostId) }

        }
    }

    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")
}
