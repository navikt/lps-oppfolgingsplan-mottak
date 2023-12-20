package no.nav.syfo.scheduling

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.service.AltinnLPSService
import org.quartz.JobBuilder.newJob
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.SimpleTrigger
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory

class AltinnLpsScheduler(
    private val database: DatabaseInterface,
    private val altinnLpsService: AltinnLPSService,
) {
    private val log = LoggerFactory.getLogger(AltinnLpsScheduler::class.qualifiedName)

    fun startScheduler(): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        log.info("[SCHEDULER]: Started scheduler")
        try {
            scheduler.start()
            val lpsRetryProcessingJob = generateLpsRetryProcessingJob()
            val lpsRetrySendToGpJob = generateLpsRetrySendToGpJob()
            scheduler.scheduleJob(lpsRetryProcessingJob.first, lpsRetryProcessingJob.second)
            scheduler.scheduleJob(lpsRetrySendToGpJob.first, lpsRetrySendToGpJob.second)
            return scheduler
        } catch (e: SchedulerException) {
            log.error("[SCHEDULER]: SchedulerException encounter while running job ${e.message}", e)
            scheduler.shutdown()
            throw e
        }
    }

    fun generateLpsRetryProcessingJob(): Pair<JobDetail, SimpleTrigger> {
        val lpsRetryProcessingJob = newJob(AltinnLpsRetryProcessingJob::class.java)
            .withIdentity(LPS_RETRY_PROCESSING_JOB, LPS_RETRY_GROUP)
            .build()
        lpsRetryProcessingJob.jobDataMap[DB_SHORTNAME] = database
        lpsRetryProcessingJob.jobDataMap[LPS_SERVICE_SHORTNAME] = altinnLpsService
        val lpsRetryProcessingTrigger = newTrigger()
            .withIdentity(LPS_RETRY_PROCESSING_TRIGGER, LPS_RETRY_GROUP)
            .startNow()
            .withSchedule(simpleSchedule()
                .withIntervalInMinutes(LPS_RETRY_PROCESSING_INTERVAL_MINUTES)
                .repeatForever())
            .build()
        return Pair(lpsRetryProcessingJob, lpsRetryProcessingTrigger)
    }

    fun generateLpsRetrySendToGpJob(): Pair<JobDetail, SimpleTrigger> {
        val lpsRetrySendToGpJob = newJob(AltinnLpsRetrySendToGpJob::class.java)
            .withIdentity(LPS_RETRY_SEND_TO_GP_JOB, LPS_RETRY_GROUP)
            .build()
        lpsRetrySendToGpJob.jobDataMap[DB_SHORTNAME] = database
        lpsRetrySendToGpJob.jobDataMap[LPS_SERVICE_SHORTNAME] = altinnLpsService
        val lpsRetrySendToGpTrigger = newTrigger()
            .withIdentity(LPS_RETRY_SEND_TO_GP_TRIGGER, LPS_RETRY_GROUP)
            .startNow()
            .withSchedule(simpleSchedule()
                .withIntervalInMinutes(LPS_RETRY_SEND_TO_GP_INTERVAL_MINUTES)
                .repeatForever())
            .build()
        return Pair(lpsRetrySendToGpJob, lpsRetrySendToGpTrigger)
    }

    companion object {
        const val LPS_RETRY_GROUP = "lpsRetryGroup"

        const val LPS_RETRY_PROCESSING_TRIGGER = "lpsRetryProcessingTrigger"
        const val LPS_RETRY_PROCESSING_JOB = "lpsRetryProcessingJob"
        const val LPS_RETRY_PROCESSING_INTERVAL_MINUTES = 5

        const val LPS_RETRY_SEND_TO_GP_TRIGGER = "lpsRetrySendToGpTrigger"
        const val LPS_RETRY_SEND_TO_GP_JOB = "lpsRetrySendToGpJob"
        const val LPS_RETRY_SEND_TO_GP_INTERVAL_MINUTES = 10
    }
}
