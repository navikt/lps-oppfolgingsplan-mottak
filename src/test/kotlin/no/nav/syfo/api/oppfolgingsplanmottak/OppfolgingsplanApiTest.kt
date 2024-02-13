package no.nav.syfo.api.oppfolgingsplanmottak

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.mockdata.createDefaultOppfolgingsplanDTOMock
import no.nav.syfo.oppfolgingsplanmottak.database.storeLpsPdf
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.oppfolgingsplanmottak.successText
import no.nav.syfo.util.configureTestApplication
import no.nav.syfo.util.validMaskinportenToken
import no.nav.syfo.veileder.database.getOppfolgingsplanerMetadataForVeileder
import java.time.LocalDate
import java.util.*

class OppfolgingsplanApiTest : DescribeSpec({

    describe("Retrieval of oppfølgingsplaner") {
        val employeeIdentificationNumber = "12345678912"
        val employeeOrgnumber = "123456789"

        it("Should get a dummy response for POST") {
            testApplication {
                val client = configureTestApplication().httpClient

                val oppfolgingsplanDTO = createDefaultOppfolgingsplanDTOMock()
                val response = client.post("/api/v1/lps/write") {
                    bearerAuth(validMaskinportenToken())
                    contentType(ContentType.Application.Json)
                    setBody(oppfolgingsplanDTO)
                }
                val virksomhetsnavn = oppfolgingsplanDTO.oppfolgingsplanMeta.virksomhet.virksomhetsnavn
                response shouldHaveStatus HttpStatusCode.OK
                response.bodyAsText() shouldContain successText(virksomhetsnavn)
            }
        }

        it("Submits and stores a follow-up plan") {
            testApplication {
                val (embeddedDatabase, client) = configureTestApplication()

                val followUpPlanDTO = createFollowUpPlan(employeeIdentificationNumber)

                val response = client.post("/api/v1/followupplan/write") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }

                val responseBody = response.body<FollowUpPlanResponse>()
                embeddedDatabase.storeLpsPdf(UUID.fromString(responseBody.uuid), byteArrayOf(0x2E, 0x38))

                val storedMetaData =
                    embeddedDatabase.getOppfolgingsplanerMetadataForVeileder(PersonIdent(employeeIdentificationNumber))

                storedMetaData.size shouldBe 1
                storedMetaData[0].fnr shouldBe employeeIdentificationNumber
                storedMetaData[0].virksomhetsnummer shouldBe employeeOrgnumber

                response shouldHaveStatus HttpStatusCode.OK
            }
        }
    }
})

private fun createFollowUpPlan(employeeIdentificationNumber: String): FollowUpPlanDTO {
    val followUpPlanDTO = FollowUpPlanDTO(
        employeeIdentificationNumber = employeeIdentificationNumber,
        typicalWorkday = "Typical workday description",
        tasksThatCanStillBeDone = "Tasks that can still be done",
        tasksThatCanNotBeDone = "Tasks that cannot be done",
        previousFacilitation = "Previous facilitation description",
        plannedFacilitation = "Planned facilitation description",
        otherFacilitationOptions = "Other facilitation options",
        followUp = "Follow up description",
        evaluationDate = LocalDate.now(),
        sendPlanToNav = true,
        needsHelpFromNav = false,
        needsHelpFromNavDescription = null,
        sendPlanToGeneralPractitioner = true,
        messageToGeneralPractitioner = "Message to general practitioner",
        additionalInformation = "Additional information",
        contactPersonFullName = "Contact person full name",
        contactPersonPhoneNumber = "12345678",
        employeeHasContributedToPlan = true,
        employeeHasNotContributedToPlanDescription = null,
        lpsName = "LPS name"
    )
    return followUpPlanDTO
}
