package no.nav.syfo.oppfolgingsplanmottak.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.db.TestDB
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanInbox
import java.time.LocalDateTime

class FollowUpPlanInboxDaoTest :
    DescribeSpec({
        val testDb = TestDB.database

        beforeTest {
            TestDB.clearAllData()
        }

        describe("FollowUpPlanInboxDao") {
            it("stores payload and orgnumbers for received follow up plans") {
                val receivedAt = LocalDateTime.of(2024, 1, 10, 12, 0, 0)
                val followUpPlanInbox =
                    FollowUpPlanInbox(
                        correlationId = "call-id-1",
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        employeeIdentificationNumber = "12345678901",
                        rawPayload = """{"payload":"raw"}""",
                        receivedAt = receivedAt,
                    )

                testDb.storeFollowUpPlanInbox(followUpPlanInbox)

                testDb.countFollowUpPlanInboxRows() shouldBe 1
                testDb.getLatestFollowUpPlanInbox() shouldBe followUpPlanInbox
            }

            it("deletes inbox entries older than 14 days and returns number of deleted rows") {
                val now = LocalDateTime.now().withNano(0)
                val oldInboxEntry =
                    FollowUpPlanInbox(
                        correlationId = "call-id-old",
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        employeeIdentificationNumber = "12345678901",
                        rawPayload = """{"payload":"old"}""",
                        receivedAt = now.minusDays(14).minusMinutes(1),
                    )
                val freshInboxEntry =
                    FollowUpPlanInbox(
                        correlationId = "call-id-fresh",
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        employeeIdentificationNumber = "12345678901",
                        rawPayload = """{"payload":"fresh"}""",
                        receivedAt = now.minusDays(14).plusMinutes(1),
                    )

                testDb.storeFollowUpPlanInbox(oldInboxEntry)
                testDb.storeFollowUpPlanInbox(freshInboxEntry)

                testDb.deleteFollowUpPlanInboxRowsOlderThan14Days() shouldBe 1
                testDb.countFollowUpPlanInboxRows() shouldBe 1
                testDb.getLatestFollowUpPlanInbox()?.correlationId shouldBe freshInboxEntry.correlationId
            }

            it("returns zero when there are no expired inbox entries") {
                testDb.deleteFollowUpPlanInboxRowsOlderThan14Days() shouldBe 0
            }
        }
    })
