package no.nav.syfo.oppfolgingsplanmottak

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.mockdata.createDefaultFollowUpPlanMockDTO
import no.nav.syfo.mockdata.randomFollowUpPlanMockDTO
import no.nav.syfo.oppfolgingsplanmottak.database.storeLpsPdf
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.util.configureTestApplication
import no.nav.syfo.util.customMaskinportenToken
import no.nav.syfo.util.validMaskinportenToken
import no.nav.syfo.veileder.database.getOppfolgingsplanerMetadataForVeileder
import java.util.*

class FollowUpPlanApiTest : DescribeSpec({
    val employeeIdentificationNumber = "12345678912"
    val employeeOrgnumber = "123456789"

    beforeSpec {
    }

    beforeTest {
        clearAllMocks()
    }

    describe("Retrieval of oppfølgingsplaner") {
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

        it("10 digits invalid employeeIdentificationNumber in follow-up plan should return bad request") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    employeeIdentificationNumber = "1234567890"
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Invalid employee identification number"
            }
        }

        it("12 digits invalid employeeIdentificationNumber in follow-up plan should return bad request") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    employeeIdentificationNumber = "123456789012"
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Invalid employee identification number"
            }
        }

        it(
            "11 digits contains invalid digit in employeeIdentificationNumber in follow-up plan should return bad request"
        ) {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    employeeIdentificationNumber = "12345678901q"
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Invalid employee identification number"
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

        it("Fails when no plan with requested uuid is found") {
            testApplication {
                val (_, client) = configureTestApplication()
                val uuid = UUID.randomUUID()
                val response = client.get("/api/v1/followupplan/$uuid/sendingstatus") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = employeeOrgnumber))
                    contentType(ContentType.Application.Json)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.NotFound
                responseMessage shouldContain "The follow-up plan with a given uuid was not found"
            }
        }
    }

    describe("Verification of integration") {
        it("Fails when consumer is missing") {
            testApplication {
                val (_, client) = configureTestApplication()

                val response = client.get("/api/v1/followupplan/verify-integration") {
                    bearerAuth(customMaskinportenToken(consumerOrgnumber = null))
                    contentType(ContentType.Application.Json)
                }

                response shouldHaveStatus HttpStatusCode.Unauthorized
                val responseBody = response.body<String>()
                responseBody shouldBe "Missing consumer claim in JWT"
            }
        }

        it("Fails when supplier is missing") {
            testApplication {
                val (_, client) = configureTestApplication()

                val response = client.get("/api/v1/followupplan/verify-integration") {
                    bearerAuth(customMaskinportenToken(supplierOrgnumber = null))
                    contentType(ContentType.Application.Json)
                }

                response shouldHaveStatus HttpStatusCode.Unauthorized
                val responseBody = response.body<String>()
                responseBody shouldBe "Missing supplier claim in JWT"
            }
        }

        it("Fails when scope is wrong") {
            testApplication {
                val (_, client) = configureTestApplication()

                val response = client.get("/api/v1/followupplan/verify-integration") {
                    bearerAuth(customMaskinportenToken(scope = "hei"))
                    contentType(ContentType.Application.Json)
                }

                response shouldHaveStatus HttpStatusCode.Unauthorized
                val responseBody = response.body<String>()
                responseBody shouldBe "Invalid scope in JWT"
            }
        }
    }
})
