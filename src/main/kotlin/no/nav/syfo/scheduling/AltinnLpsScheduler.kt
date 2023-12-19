package no.nav.syfo.scheduling

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.service.AltinnLPSService
import org.quartz.JobBuilder.newJob
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleScheduleBuilder.simpleSchedule
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
        log.info("Started scheduler")
        try {
            scheduler.start()
            val lpsRetryJob = newJob(AltinnLpsRetryProcessingJob::class.java)
                .withIdentity(LPS_RETRY_JOB, LPS_RETRY_GROUP)
                .build()
            lpsRetryJob.jobDataMap[DB_SHORTNAME] = database
            lpsRetryJob.jobDataMap[LPS_SERVICE_SHORTNAME] = altinnLpsService
            val lpsRetryTrigger = newTrigger()
                .withIdentity(LPS_RETRY_TRIGGER, LPS_RETRY_GROUP)
                .startNow()
                    .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(LPS_RETRY_INTERVAL_MINUTES)
                        .repeatForever())
                .build()
            scheduler.scheduleJob(lpsRetryJob, lpsRetryTrigger)
            return scheduler
        } catch (e: SchedulerException) {
            log.error("[SCHEDULER]: SchedulerException encounter while running job ${e.message}", e)
            scheduler.shutdown()
            throw e
        }
    }

    companion object {
        const val LPS_RETRY_GROUP = "lpsRetryGroup"
        const val LPS_RETRY_TRIGGER = "lpsRetryTrigger"
        const val LPS_RETRY_JOB = "lpsRetryJOB"
        const val LPS_RETRY_INTERVAL_MINUTES = 5
    }
}
