package no.nav.syfo.scheduling

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.service.AltinnLPSService
import no.nav.syfo.util.LeaderElection
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
    private val leaderElection: LeaderElection,
) {
    private val log = LoggerFactory.getLogger(AltinnLpsScheduler::class.qualifiedName)

    fun startScheduler(): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        log.info("[SCHEDULER]: Started scheduler")
        try {
            scheduler.start()
            val lpsRetryProcessLpsJob = generateLpsRetryProcessLpsJob()
            val lpsRetryForwardLpsJob = generateLpsRetryForwardLpsJob()
            scheduler.scheduleJob(lpsRetryProcessLpsJob.first, lpsRetryProcessLpsJob.second)
            scheduler.scheduleJob(lpsRetryForwardLpsJob.first, lpsRetryForwardLpsJob.second)
            return scheduler
        } catch (e: SchedulerException) {
            log.error("[SCHEDULER]: SchedulerException encounter while running job ${e.message}", e)
            scheduler.shutdown()
            throw e
        }
    }

    fun generateLpsRetryProcessLpsJob(): Pair<JobDetail, SimpleTrigger> {
        val lpsRetryProcessLpsJob = newJob(AltinnLpsRetryProcessLpsJob::class.java)
            .withIdentity(LPS_RETRY_PROCESS_LPS_JOB, LPS_RETRY_GROUP)
            .build()
        lpsRetryProcessLpsJob.jobDataMap[DB_SHORTNAME] = database
        lpsRetryProcessLpsJob.jobDataMap[LPS_SERVICE_SHORTNAME] = altinnLpsService
        lpsRetryProcessLpsJob.jobDataMap[LEADER_ELECTION_SHORTNAME] = leaderElection
        val lpsRetryProcessLpsTrigger = newTrigger()
            .withIdentity(LPS_RETRY_PROCESS_LPS_TRIGGER, LPS_RETRY_GROUP)
            .startNow()
            .withSchedule(simpleSchedule()
                .withIntervalInMinutes(LPS_RETRY_PROCESS_LPS_INTERVAL_MINUTES)
                .repeatForever())
            .build()
        return Pair(lpsRetryProcessLpsJob, lpsRetryProcessLpsTrigger)
    }

    fun generateLpsRetryForwardLpsJob(): Pair<JobDetail, SimpleTrigger> {
        val lpsRetryForwardLpsJob = newJob(AltinnLpsRetryForwardLpsJob::class.java)
            .withIdentity(LPS_RETRY_FORWARD_LPS_JOB, LPS_RETRY_GROUP)
            .build()
        lpsRetryForwardLpsJob.jobDataMap[DB_SHORTNAME] = database
        lpsRetryForwardLpsJob.jobDataMap[LPS_SERVICE_SHORTNAME] = altinnLpsService
        lpsRetryForwardLpsJob.jobDataMap[LEADER_ELECTION_SHORTNAME] = leaderElection
        val lpsRetryForwardLpsTrigger = newTrigger()
            .withIdentity(LPS_RETRY_FORWARD_LPS_TRIGGER, LPS_RETRY_GROUP)
            .startNow()
            .withSchedule(simpleSchedule()
                .withIntervalInMinutes(LPS_RETRY_FORWARD_LPS_INTERVAL_MINUTES)
                .repeatForever())
            .build()
        return Pair(lpsRetryForwardLpsJob, lpsRetryForwardLpsTrigger)
    }

    companion object {
        const val LPS_RETRY_GROUP = "lpsRetryGroup"

        const val LPS_RETRY_PROCESS_LPS_TRIGGER = "lpsRetryProcessLpsTrigger"
        const val LPS_RETRY_PROCESS_LPS_JOB = "lpsRetryProcessLpsJob"
        const val LPS_RETRY_PROCESS_LPS_INTERVAL_MINUTES = 5

        const val LPS_RETRY_FORWARD_LPS_TRIGGER = "lpsRetryForwardLpsTrigger"
        const val LPS_RETRY_FORWARD_LPS_JOB = "lpsRetryForwardLpsJob"
        const val LPS_RETRY_FORWARD_LPS_INTERVAL_MINUTES = 3
    }
}
