package no.nav.syfo.oppfolgingsplanmottak.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.db.TestDB
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanInbox
import no.nav.syfo.oppfolgingsplanmottak.domain.InboxStatus
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
                        status = InboxStatus.RECEIVED,
                        statusMessage = null,
                        receivedAt = receivedAt,
                        validatedAt = null,
                        processedAt = null,
                        updatedAt = receivedAt,
                    )

                testDb.storeFollowUpPlanInbox(followUpPlanInbox)

                testDb.getFollowUpPlanInbox(followUpPlanInbox.correlationId) shouldBe followUpPlanInbox
            }

            it("updates status and timestamps for processed follow up plans") {
                val receivedAt = LocalDateTime.of(2024, 1, 10, 12, 0, 0)
                val validatedAt = receivedAt.plusMinutes(1)
                val processedAt = validatedAt.plusMinutes(1)
                val correlationId = "call-id-2"

                testDb.storeFollowUpPlanInbox(
                    FollowUpPlanInbox(
                        correlationId = correlationId,
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        rawPayload = """{"payload":"raw"}""",
                        status = InboxStatus.RECEIVED,
                        statusMessage = null,
                        receivedAt = receivedAt,
                        validatedAt = null,
                        processedAt = null,
                        updatedAt = receivedAt,
                    ),
                )

                val rowsUpdated =
                    testDb.updateFollowUpPlanInboxStatus(
                        correlationId = correlationId,
                        status = InboxStatus.PROCESSED,
                        statusMessage = "Processed successfully",
                        validatedAt = validatedAt,
                        processedAt = processedAt,
                    )

                rowsUpdated shouldBe 1

                val updatedInbox = testDb.getFollowUpPlanInbox(correlationId)
                updatedInbox?.status shouldBe InboxStatus.PROCESSED
                updatedInbox?.statusMessage shouldBe "Processed successfully"
                updatedInbox?.validatedAt shouldBe validatedAt
                updatedInbox?.processedAt shouldBe processedAt
                updatedInbox?.organizationNumber shouldBe "123456789"
                updatedInbox?.lpsOrgnumber shouldBe "987654321"
            }
        }
    })
