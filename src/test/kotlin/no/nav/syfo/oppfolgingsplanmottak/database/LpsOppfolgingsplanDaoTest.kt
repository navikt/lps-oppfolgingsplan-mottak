package no.nav.syfo.oppfolgingsplanmottak.database

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.sql.SQLException
import java.util.*
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.createDefaultFollowUpPlan

class LpsOppfolgingsplanDaoTest : DescribeSpec({

    describe("LpsOppfolgingsplanDaoSpek") {
        val uuid = UUID.randomUUID()
        val embeddedDatabase = EmbeddedDatabase()
        beforeTest {
            embeddedDatabase.dropData()
        }

        it("Should throw exception if no plan with requested uuid is found") {
            shouldThrow<SQLException> {
                embeddedDatabase.findFollowUpPlanResponseById(UUID.randomUUID())
            }
        }

        it("Should store plan with a given uuid") {
            embeddedDatabase.storeFollowUpPlan(
                uuid = uuid,
                lpsOrgnumber = "123",
                organizationNumber = "456",
                followUpPlanDTO = createDefaultFollowUpPlan("12121212121"),
                sentToNavAt = null,
                sentToGeneralPractitionerAt = null,
            )
            val fetchedPlan = embeddedDatabase.findSendingStatus(uuid)
            fetchedPlan shouldNotBe null
            fetchedPlan?.uuid shouldBe uuid.toString()
        }

        it("Should return null if plan with a given uuid is not found") {
            embeddedDatabase.storeFollowUpPlan(
                uuid = uuid,
                lpsOrgnumber = "123",
                organizationNumber = "456",
                followUpPlanDTO = createDefaultFollowUpPlan("12121212121"),
                sentToNavAt = null,
                sentToGeneralPractitionerAt = null,
            )
            val fetchedPlan = embeddedDatabase.findSendingStatus(UUID.randomUUID())
            fetchedPlan shouldBe null
        }
    }

})
