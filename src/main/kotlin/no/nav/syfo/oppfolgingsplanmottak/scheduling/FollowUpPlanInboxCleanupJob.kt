package no.nav.syfo.oppfolgingsplanmottak.scheduling

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.scheduling.DB_SHORTNAME
import no.nav.syfo.application.scheduling.LEADER_ELECTION_SHORTNAME
import no.nav.syfo.oppfolgingsplanmottak.database.deleteFollowUpPlanInboxRowsOlderThan14Days
import no.nav.syfo.util.LeaderElection
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class FollowUpPlanInboxCleanupJob : Job {
    private val log = LoggerFactory.getLogger(FollowUpPlanInboxCleanupJob::class.qualifiedName)
    private val jobName = "FOLLOW_UP_PLAN_INBOX_CLEANUP_JOB"
    private val jobLogPrefix = "[$jobName]:"

    override fun execute(context: JobExecutionContext) {
        val jobDataMap = context.jobDetail.jobDataMap
        val database = jobDataMap[DB_SHORTNAME] as DatabaseInterface
        val leaderElection = jobDataMap[LEADER_ELECTION_SHORTNAME] as LeaderElection

        if (leaderElection.thisPodIsLeader()) {
            logInfo("Starting job $jobName")
            val deletedRows = database.deleteFollowUpPlanInboxRowsOlderThan14Days()
            logInfo("Deleted $deletedRows inbox rows older than 14 days")
            logInfo("$jobName job successfully finished")
        }
    }

    private fun logInfo(message: String) = log.info("$jobLogPrefix $message")
}
