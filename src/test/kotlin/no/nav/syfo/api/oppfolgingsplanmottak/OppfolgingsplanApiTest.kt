package no.nav.syfo.api.oppfolgingsplanmottak

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.util.*
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.oppfolgingsplanmottak.database.storeLpsPdf
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.util.configureTestApplication
import no.nav.syfo.util.validMaskinportenToken
import no.nav.syfo.veileder.database.getOppfolgingsplanerMetadataForVeileder

class OppfolgingsplanApiTest : DescribeSpec({
    val isdialogmeldingConsumer = mockk<IsdialogmeldingClient>(relaxed = true)

    describe("Retrieval of oppfølgingsplaner") {
        val employeeIdentificationNumber = "12345678912"
        val employeeOrgnumber = "123456789"

        it("Submits and stores a follow-up plan") {
            testApplication {
                val (embeddedDatabase, client) = configureTestApplication()

                val followUpPlanDTO = createFollowUpPlan(employeeIdentificationNumber)
//                coJustRun { isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(any(), any())}
                coEvery { isdialogmeldingConsumer.sendLpsPlanToGeneralPractitioner(any(), any()) } returns true
                val response = client.post("/api/v1/followupplan") {
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
