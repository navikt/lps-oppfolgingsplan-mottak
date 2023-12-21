package no.nav.syfo.scheduling

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.getLpsNotYetSentToGp
import no.nav.syfo.db.updateSendToGpRetryCount
import no.nav.syfo.metrics.COUNT_METRIKK_DELT_MED_FASTLEGE_ETTER_FEILET_SENDING
import no.nav.syfo.metrics.COUNT_METRIKK_PROSSESERING_VELLYKKET
import no.nav.syfo.service.AltinnLPSService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class AltinnLpsRetrySendToGpJob: Job {
    private val log = LoggerFactory.getLogger(AltinnLpsRetrySendToGpJob::class.qualifiedName)
    private val jobName = "RETRY_SEND_TO_GP_JOB"
    private val jobLogPrefix = "[$jobName]:"

    override fun execute(context: JobExecutionContext) {
        logInfo("Starting job $jobName")
        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val altinnLpsService = jobDataMap[LPS_SERVICE_SHORTNAME] as AltinnLPSService
        retrySendToGp(database, altinnLpsService)
        logInfo("$jobName job successfully finished")
    }

    private fun retrySendToGp(
        database: DatabaseInterface,
        altinnLpsService: AltinnLPSService,
    ) {
        val retrySendThreshold = altinnLpsService.sendToGpRetryThreshold()
        val lpsNotYetSentToGp = database.getLpsNotYetSentToGp(retrySendThreshold)
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

    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")
}
