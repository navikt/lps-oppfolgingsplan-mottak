package no.nav.syfo.scheduling

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.getLpsWithoutGeneratedPdf
import no.nav.syfo.db.getLpsWithoutMostRecentFnr
import no.nav.syfo.service.AltinnLPSService
import no.nav.syfo.util.LeaderElection
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class AltinnLpsRetryProcessLpsJob: Job {
    private val log = LoggerFactory.getLogger(AltinnLpsRetryProcessLpsJob::class.qualifiedName)
    private val jobName = "RETRY_PROCESS_LPS_JOB"
    private val jobLogPrefix = "[$jobName]:"
    override fun execute(context: JobExecutionContext) {

        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val altinnLpsService = jobDataMap[LPS_SERVICE_SHORTNAME] as AltinnLPSService
        val leaderElection = jobDataMap[LEADER_ELECTION_SHORTNAME] as LeaderElection
        if (leaderElection.thisPodIsLeader()) {
            logInfo("Starting job $jobName")
            retryStoreFnrs(database, altinnLpsService)
            retryStorePdfs(database, altinnLpsService)
            logInfo("$jobName job successfully finished")
        }
    }

    private fun retryStoreFnrs(database: DatabaseInterface, altinnLpsService: AltinnLPSService) {
        val lpsWithoutMostRecentFnr = database.getLpsWithoutMostRecentFnr()
        val nrLpsWithoutMostRecentFnr = lpsWithoutMostRecentFnr.size
        if (nrLpsWithoutMostRecentFnr == 0) {
            return
        }
        var successfulRetries = 0
        lpsWithoutMostRecentFnr.forEach { lps ->
            if (altinnLpsService.retryStoreFnr(lps.uuid, lps.lpsFnr)) {
                successfulRetries++
            }
        }
        logInfo("$successfulRetries/${nrLpsWithoutMostRecentFnr} fnrs successfully retried and stored")
    }

    private fun retryStorePdfs(
        database: DatabaseInterface,
        altinnLpsService: AltinnLPSService,
    ) {
        val lpsWithoutPdfs = database.getLpsWithoutGeneratedPdf()
        val nrLpsWithoutPdfs = lpsWithoutPdfs.size
        if (nrLpsWithoutPdfs == 0) {
            return
        }
        var successfulRetries = 0
        lpsWithoutPdfs.forEach { lps ->
            if (altinnLpsService.retryStorePdf(
                lps.uuid,
                lps.fnr!!,
                lps.xml,
            )) { successfulRetries++ }
        }
        logInfo("$successfulRetries/${nrLpsWithoutPdfs} PDFs successfully generated and stored")
    }

    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")
}
