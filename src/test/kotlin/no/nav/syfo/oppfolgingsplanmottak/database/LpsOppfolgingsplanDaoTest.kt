package no.nav.syfo.oppfolgingsplanmottak.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.db.TestDB
import no.nav.syfo.mockdata.createDefaultFollowUpPlan
import java.util.*

class LpsOppfolgingsplanDaoTest :
    DescribeSpec({
        describe("LpsOppfolgingsplanDaoSpek") {
            val uuid = UUID.randomUUID()
            val testDb = TestDB.database
            beforeTest {
                TestDB.clearAllData()
            }

            it("Should store plan with a given uuid") {
                testDb.storeFollowUpPlan(
                    uuid = uuid,
                    lpsOrgnumber = "123",
                    organizationNumber = "456",
                    followUpPlanDTO = createDefaultFollowUpPlan("12121212121"),
                    sentToNavAt = null,
                    sentToGeneralPractitionerAt = null,
                )
                val fetchedPlan = testDb.findFollowUpPlanResponseById(uuid)
                fetchedPlan shouldNotBe null
                fetchedPlan?.uuid shouldBe uuid.toString()
            }

            it("Should return null if plan with a given uuid is not found") {
                testDb.storeFollowUpPlan(
                    uuid = uuid,
                    lpsOrgnumber = "123",
                    organizationNumber = "456",
                    followUpPlanDTO = createDefaultFollowUpPlan("12121212121"),
                    sentToNavAt = null,
                    sentToGeneralPractitionerAt = null,
                )
                val fetchedPlan = testDb.findFollowUpPlanResponseById(UUID.randomUUID())
                fetchedPlan shouldBe null
            }

            it("should return only unsent plans") {
                testDb.storeFollowUpPlan(
                    uuid = uuid,
                    lpsOrgnumber = "123",
                    organizationNumber = "456",
                    followUpPlanDTO = createDefaultFollowUpPlan("12121212121"),
                    sentToNavAt = null,
                    sentToGeneralPractitionerAt = null,
                )
                val fetchedPlans = testDb.findUnsentFollowUpPlan()

                fetchedPlans?.size shouldBe 1
            }
        }
    })
