package no.nav.syfo.oppfolgingsplanmottak

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import java.util.*
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.mockdata.createDefaultFollowUpPlanMockDTO
import no.nav.syfo.mockdata.randomFollowUpPlanMockDTO
import no.nav.syfo.oppfolgingsplanmottak.database.storeLpsPdf
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.util.configureTestApplication
import no.nav.syfo.util.validMaskinportenToken
import no.nav.syfo.veileder.database.getOppfolgingsplanerMetadataForVeileder

class FollowUpPlanApiTest : DescribeSpec({

    beforeSpec {
    }

    beforeTest {
        clearAllMocks()
    }

    describe("Retrieval of oppfølgingsplaner") {
        val employeeIdentificationNumber = "12345678912"
        val employeeOrgnumber = "123456789"

        it("Submits and stores a follow-up plan") {
            testApplication {
                val (embeddedDatabase, client) = configureTestApplication()

                val followUpPlanDTO = createDefaultFollowUpPlanMockDTO(employeeIdentificationNumber)

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

        it("Missing lpsName in follow-up plan should return bad request") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    lpsName = null
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Failed to convert request body"
            }
        }

        it("Fails when employee has not contributed, but description is missing") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    employeeHasContributedToPlan = false,
                    employeeHasNotContributedToPlanDescription = null
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Failed to convert request body"
            }
        }

        it("Fails when needs help from NAV is true, but description is missing") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    needsHelpFromNav = true,
                    sendPlanToNav = true,
                    needsHelpFromNavDescription = null
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Failed to convert request body"
            }
        }
    }
})

