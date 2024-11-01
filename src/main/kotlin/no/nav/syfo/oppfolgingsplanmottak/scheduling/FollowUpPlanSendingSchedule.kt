package no.nav.syfo.oppfolgingsplanmottak.scheduling

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.environment.ToggleEnv
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
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
import no.nav.syfo.application.scheduling.DB_SHORTNAME
import no.nav.syfo.application.scheduling.LEADER_ELECTION_SHORTNAME
import no.nav.syfo.application.scheduling.TOGGLES_SHORTNAME

class FollowUpPlanSendingSchedule (
    private val database: DatabaseInterface,
    private val followUpPlanSendingService: FollowUpPlanSendingService,
    private val leaderElection: LeaderElection,
    private val toggles: ToggleEnv,
) {
    private val log = LoggerFactory.getLogger(FollowUpPlanSendingSchedule::class.qualifiedName)
    fun startScheduler(): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        log.info("[SCHEDULER]: Started scheduler")
        try {
            scheduler.start()
            val followUpPlanJob = generateFollowUpPlanJob()
            scheduler.scheduleJob(followUpPlanJob.first, followUpPlanJob.second)
            return scheduler
        } catch (e: SchedulerException) {
            log.error("[SCHEDULER]: SchedulerException encounter while running job ${e.message}", e)
            scheduler.shutdown()
            throw e
        }
    }

    private fun generateFollowUpPlanJob(): Pair<JobDetail, SimpleTrigger> {
        val followUpPlanRetryJob = newJob(FollowUpPlanRetryJob::class.java)
            .withIdentity(FOLLOW_UP_PLAN_JOB, FOLLOW_UP_PLAN_GROUP)
            .build()
        followUpPlanRetryJob.jobDataMap[DB_SHORTNAME] = database
        followUpPlanRetryJob.jobDataMap[FOLLOW_UP_PLAN_SERVICE_SHORTNAME] = followUpPlanSendingService
        followUpPlanRetryJob.jobDataMap[LEADER_ELECTION_SHORTNAME] = leaderElection
        followUpPlanRetryJob.jobDataMap[TOGGLES_SHORTNAME] = toggles
        val followUpPlanTrigger = newTrigger()
            .withIdentity(FOLLOW_UP_PLAN_TRIGGER, FOLLOW_UP_PLAN_GROUP)
            .startNow()
            .withSchedule(
                simpleSchedule()
                    .withIntervalInMinutes(FOLLOW_UP_PLAN_INTERVAL_IN_MINUTES)
                    .repeatForever()
            )
            .build()
        return Pair(followUpPlanRetryJob, followUpPlanTrigger)
    }

    companion object {
        const val FOLLOW_UP_PLAN_GROUP = "FollowUpPlanGroup"
        const val FOLLOW_UP_PLAN_JOB = "FollowUpPlanJob"
        const val FOLLOW_UP_PLAN_TRIGGER = "FollowUpPlanTrigger"
        const val FOLLOW_UP_PLAN_INTERVAL_IN_MINUTES = 10
        const val FOLLOW_UP_PLAN_SERVICE_SHORTNAME = "FollowUpPlanServiceShortname"
    }
}