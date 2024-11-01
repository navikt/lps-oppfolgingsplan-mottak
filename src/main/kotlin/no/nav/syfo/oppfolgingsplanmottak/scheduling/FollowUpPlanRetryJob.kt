package no.nav.syfo.oppfolgingsplanmottak.scheduling

import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.scheduling.DB_SHORTNAME
import no.nav.syfo.application.scheduling.LEADER_ELECTION_SHORTNAME
import no.nav.syfo.oppfolgingsplanmottak.database.findUnsentFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.scheduling.FollowUpPlanSendingSchedule.Companion.FOLLOW_UP_PLAN_SERVICE_SHORTNAME
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.util.LeaderElection
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class FollowUpPlanRetryJob: Job {
    private val log = LoggerFactory.getLogger(FollowUpPlanRetryJob::class.qualifiedName)
    private val jobName = "FOLLOW_UP_PLAN_JOB"
    private val jobLogPrefix = "[$jobName]:"
    override fun execute(context: JobExecutionContext) {
        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val followUpPlanSendingService = jobDataMap[FOLLOW_UP_PLAN_SERVICE_SHORTNAME] as FollowUpPlanSendingService
        val leaderElection = jobDataMap[LEADER_ELECTION_SHORTNAME] as LeaderElection
        if (leaderElection.thisPodIsLeader()) {
            logInfo("Starting job $jobName")
            runBlocking {
                retryFollowUpPlanSendingService(database, followUpPlanSendingService)
                // TODO Implement retry FollowUpPlanSendingService
            }
            logInfo("$jobName job successfully finished")
        }
    }

    private suspend fun retryFollowUpPlanSendingService(
        database: DatabaseInterface,
        followUpPlanSendingService: FollowUpPlanSendingService,
    ) {
        val unsentFollowUpPlans = database.findUnsentFollowUpPlan()
        unsentFollowUpPlans.
        // TODO Implement retry FollowUpPlanSendingService
    }


    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")

}