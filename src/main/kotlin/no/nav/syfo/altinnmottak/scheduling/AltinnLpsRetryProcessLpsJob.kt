package no.nav.syfo.altinnmottak.scheduling

import kotlinx.coroutines.runBlocking
import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.altinnmottak.database.getAltinnLpsOppfolgingsplanWithoutGeneratedPdf
import no.nav.syfo.altinnmottak.database.getAltinnLpsOppfolgingsplanWithoutMostRecentFnr
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.scheduling.DB_SHORTNAME
import no.nav.syfo.application.scheduling.LEADER_ELECTION_SHORTNAME
import no.nav.syfo.util.LeaderElection
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class AltinnLpsRetryProcessLpsJob: Job {
    private val log = LoggerFactory.getLogger(AltinnLpsRetryProcessLpsJob::class.qualifiedName)
    private val jobName = "PROCESS_LPS_JOB"
    private val jobLogPrefix = "[$jobName]:"
    override fun execute(context: JobExecutionContext) {

        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val altinnLpsService = jobDataMap[LPS_SERVICE_SHORTNAME] as AltinnLpsService
        val leaderElection = jobDataMap[LEADER_ELECTION_SHORTNAME] as LeaderElection
        if (leaderElection.thisPodIsLeader()) {
            logInfo("Starting job $jobName")
            runBlocking {
                retryStoreFnrs(database, altinnLpsService)
                retryStorePdfs(database, altinnLpsService)
            }
            logInfo("$jobName job successfully finished")
        }
    }

    private suspend fun retryStoreFnrs(database: DatabaseInterface, altinnLpsService: AltinnLpsService) {
        val lpsWithoutMostRecentFnr = database.getAltinnLpsOppfolgingsplanWithoutMostRecentFnr()
        val lpsPlansWithoutMostRecentFnrSize = lpsWithoutMostRecentFnr.size
        if (lpsPlansWithoutMostRecentFnrSize == 0) {
            return
        }
        var successfulRetriesCount = 0
        lpsWithoutMostRecentFnr.forEach { lps ->
            if (altinnLpsService.retryStoreFnr(lps.uuid, lps.lpsFnr)) {
                successfulRetriesCount++
            }
        }
        logInfo("$successfulRetriesCount/${lpsPlansWithoutMostRecentFnrSize} fnrs successfully retried and stored")
    }

    private suspend fun retryStorePdfs(
        database: DatabaseInterface,
        altinnLpsService: AltinnLpsService,
    ) {
        val lpsWithoutPdfs = database.getAltinnLpsOppfolgingsplanWithoutGeneratedPdf()
        val lpsWithoutPdfsSize = lpsWithoutPdfs.size
        if (lpsWithoutPdfsSize == 0) {
            return
        }
        var successfulRetriesCount = 0
        lpsWithoutPdfs.forEach { lps ->
            if (altinnLpsService.retryStorePdf(
                lps.uuid,
                lps.fnr!!,
                lps.xml,
            )) { successfulRetriesCount++ }
        }
        logInfo("$successfulRetriesCount/${lpsWithoutPdfsSize} PDFs successfully generated and stored")
    }

    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")
}
