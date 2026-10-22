package no.nav.syfo.altinnmottak.scheduling

import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.application.scheduling.DB_SHORTNAME
import no.nav.syfo.application.scheduling.LEADER_ELECTION_SHORTNAME
import no.nav.syfo.application.scheduling.TOGGLES_SHORTNAME
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
    private val altinnLpsService: AltinnLpsService,
    private val leaderElection: LeaderElection,
    private val toggles: ToggleEnv,
) {
    private val log = LoggerFactory.getLogger(AltinnLpsScheduler::class.qualifiedName)

    fun startScheduler(): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        log.info("[SCHEDULER]: Started scheduler")
        try {
            scheduler.start()
            val retryForwardLpsPlanJob = generateRetryForwardLpsPlanJob()
            scheduler.scheduleJob(retryForwardLpsPlanJob.first, retryForwardLpsPlanJob.second)
            return scheduler
        } catch (e: SchedulerException) {
            log.error("[SCHEDULER]: SchedulerException encounter while running job ${e.message}", e)
            scheduler.shutdown()
            throw e
        }
    }

    private fun generateRetryForwardLpsPlanJob(): Pair<JobDetail, SimpleTrigger> {
        val lpsRetryForwardLpsJob =
            newJob(AltinnLpsRetryForwardLpsJob::class.java)
                .withIdentity(RETRY_FORWARD_LPS_PLAN_JOB, ALTINN_LPS_PLAN_GROUP)
                .build()
        lpsRetryForwardLpsJob.jobDataMap[DB_SHORTNAME] = database
        lpsRetryForwardLpsJob.jobDataMap[LPS_SERVICE_SHORTNAME] = altinnLpsService
        lpsRetryForwardLpsJob.jobDataMap[LEADER_ELECTION_SHORTNAME] = leaderElection
        lpsRetryForwardLpsJob.jobDataMap[TOGGLES_SHORTNAME] = toggles
        val lpsRetryForwardLpsTrigger =
            newTrigger()
                .withIdentity(RETRY_FORWARD_LPS_PLAN_TRIGGER, ALTINN_LPS_PLAN_GROUP)
                .startNow()
                .withSchedule(
                    simpleSchedule()
                        .withIntervalInMinutes(RETRY_FORWARD_LPS_PLAN_INTERVAL_IN_MINUTES)
                        .repeatForever(),
                ).build()
        return Pair(lpsRetryForwardLpsJob, lpsRetryForwardLpsTrigger)
    }

    companion object {
        const val ALTINN_LPS_PLAN_GROUP = "AltinnLpsPlanGroup"

        const val RETRY_FORWARD_LPS_PLAN_TRIGGER = "RetryForwardLpsPlanTrigger"
        const val RETRY_FORWARD_LPS_PLAN_JOB = "RetryForwardLpsPlanJob"
        const val RETRY_FORWARD_LPS_PLAN_INTERVAL_IN_MINUTES = 10
    }
}
