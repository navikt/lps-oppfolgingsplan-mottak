package no.nav.syfo.oppfolgingsplanmottak.scheduling

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.application.scheduling.DB_SHORTNAME
import no.nav.syfo.application.scheduling.LEADER_ELECTION_SHORTNAME
import no.nav.syfo.db.TestDB
import no.nav.syfo.oppfolgingsplanmottak.database.countFollowUpPlanInboxRows
import no.nav.syfo.oppfolgingsplanmottak.database.getLatestFollowUpPlanInbox
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlanInbox
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanInbox
import no.nav.syfo.util.LeaderElection
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import java.time.LocalDateTime

class FollowUpPlanInboxCleanupJobTest :
    DescribeSpec({
        val database = TestDB.database
        val leaderElection = mockk<LeaderElection>()
        val job = FollowUpPlanInboxCleanupJob()

        beforeTest {
            TestDB.clearAllData()
        }

        fun jobExecutionContext() =
            mockk<JobExecutionContext> {
                every { jobDetail } returns
                    mockk<JobDetail> {
                        every { jobDataMap } returns
                            JobDataMap().apply {
                                put(DB_SHORTNAME, database)
                                put(LEADER_ELECTION_SHORTNAME, leaderElection)
                            }
                    }
            }

        fun followUpPlanInbox(
            correlationId: String,
            receivedAt: LocalDateTime,
        ) = FollowUpPlanInbox(
            correlationId = correlationId,
            organizationNumber = "123456789",
            lpsOrgnumber = "987654321",
            employeeIdentificationNumber = "12345678901",
            rawPayload = """{"payload":"raw"}""",
            receivedAt = receivedAt,
        )

        describe("FollowUpPlanInboxCleanupJob") {
            it("deletes old inbox rows when pod is leader") {
                val now = LocalDateTime.now().withNano(0)
                val oldInboxEntry = followUpPlanInbox("call-id-old", now.minusDays(15))
                val freshInboxEntry = followUpPlanInbox("call-id-fresh", now.minusDays(13))
                database.storeFollowUpPlanInbox(oldInboxEntry)
                database.storeFollowUpPlanInbox(freshInboxEntry)
                every { leaderElection.thisPodIsLeader() } returns true

                job.execute(jobExecutionContext())

                database.countFollowUpPlanInboxRows() shouldBe 1
                database.getLatestFollowUpPlanInbox()?.correlationId shouldBe freshInboxEntry.correlationId
            }

            it("does not delete old inbox rows when pod is not leader") {
                val oldInboxEntry = followUpPlanInbox("call-id-old", LocalDateTime.now().withNano(0).minusDays(15))
                database.storeFollowUpPlanInbox(oldInboxEntry)
                every { leaderElection.thisPodIsLeader() } returns false

                job.execute(jobExecutionContext())

                database.countFollowUpPlanInboxRows() shouldBe 1
                database.getLatestFollowUpPlanInbox()?.correlationId shouldBe oldInboxEntry.correlationId
            }
        }
    })
