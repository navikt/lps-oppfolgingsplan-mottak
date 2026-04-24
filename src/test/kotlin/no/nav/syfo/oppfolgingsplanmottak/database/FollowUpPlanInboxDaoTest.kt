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
                        rawPayload = """{"payload":"raw"}""",
                        receivedAt = receivedAt,
                    )

                testDb.storeFollowUpPlanInbox(followUpPlanInbox)

                testDb.getFollowUpPlanInbox(followUpPlanInbox.correlationId) shouldBe followUpPlanInbox
                testDb.getLatestFollowUpPlanInbox() shouldBe followUpPlanInbox
            }

            it("returns the latest stored inbox entry") {
                val receivedAt = LocalDateTime.of(2024, 1, 10, 12, 0, 0)

                testDb.storeFollowUpPlanInbox(
                    FollowUpPlanInbox(
                        correlationId = "call-id-2",
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        rawPayload = """{"payload":"raw"}""",
                        receivedAt = receivedAt,
                    ),
                )

                testDb.storeFollowUpPlanInbox(
                    FollowUpPlanInbox(
                        correlationId = "call-id-3",
                        organizationNumber = "111111111",
                        lpsOrgnumber = "222222222",
                        rawPayload = """{"payload":"newest"}""",
                        receivedAt = receivedAt.plusMinutes(1),
                    ),
                )

                testDb.getLatestFollowUpPlanInbox()?.correlationId shouldBe "call-id-3"
            }
        }
    })
