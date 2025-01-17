package no.nav.syfo.oppfolgingsplanmottak

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import no.nav.syfo.application.exception.ApiError.FollowUpPlanDTOValidationError
import no.nav.syfo.application.exception.ErrorType
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.mockdata.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.mockdata.UserConstants.ARBEIDSTAKER_FNR_NO_ARBEIDSFORHOLD
import no.nav.syfo.mockdata.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.mockdata.createDefaultFollowUpPlanMockDTO
import no.nav.syfo.mockdata.randomFollowUpPlanMockDTO
import no.nav.syfo.oppfolgingsplanmottak.database.storeLpsPdf
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import no.nav.syfo.sykmelding.database.persistSykmeldingsperiode
import no.nav.syfo.util.configureTestApplication
import no.nav.syfo.util.customMaskinportenToken
import no.nav.syfo.util.validMaskinportenToken
import no.nav.syfo.veileder.database.getOppfolgingsplanerMetadataForVeileder
import java.time.LocalDate
import java.util.*

class FollowUpPlanApiTest : DescribeSpec({
    beforeSpec {
    }

    beforeTest {
        clearAllMocks()
    }

    describe("Retrieval of oppfølgingsplaner") {
        it("Submits and stores a follow-up plan") {
            testApplication {
                val (embeddedDatabase, client) = configureTestApplication()
                embeddedDatabase.persistSykmeldingsperiode(
                    sykmeldingId = "12345",
                    orgnummer = VIRKSOMHETSNUMMER,
                    employeeIdentificationNumber = ARBEIDSTAKER_FNR,
                    fom = LocalDate.now().minusWeeks(1),
                    tom = LocalDate.now().plusWeeks(1)
                )

                val followUpPlanDTO = createDefaultFollowUpPlanMockDTO(ARBEIDSTAKER_FNR)

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }

                val responseBody = response.body<FollowUpPlanResponse>()
                embeddedDatabase.storeLpsPdf(UUID.fromString(responseBody.uuid), byteArrayOf(0x2E, 0x38))

                val storedMetaData =
                    embeddedDatabase.getOppfolgingsplanerMetadataForVeileder(PersonIdent(ARBEIDSTAKER_FNR))

                storedMetaData.size shouldBe 1
                storedMetaData[0].fnr shouldBe ARBEIDSTAKER_FNR
                storedMetaData[0].virksomhetsnummer shouldBe VIRKSOMHETSNUMMER

                response shouldHaveStatus HttpStatusCode.OK
            }
        }

        it("Missing active sendt sykmelding should return forbidden") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = createDefaultFollowUpPlanMockDTO(ARBEIDSTAKER_FNR)

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }

                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.Forbidden
                responseMessage shouldContain "No active sykmelding sent to employer"
            }
        }

        it("Missing arbeidsforhold should return forbidden") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = createDefaultFollowUpPlanMockDTO(ARBEIDSTAKER_FNR_NO_ARBEIDSFORHOLD)

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }

                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.Forbidden
                responseMessage shouldContain "No active employment relationship found for given orgnumber"
            }
        }

        it("Missing lpsName in follow-up plan should return bad request") {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    lpsName = null
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
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
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
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
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "Invalid employee identification number"
            }
        }

        it(
            "employeeIdentificationNumber with letters returns bad request"
        ) {
            testApplication {
                val (_, client) = configureTestApplication()

                val followUpPlanDTO = randomFollowUpPlanMockDTO.copy(
                    employeeIdentificationNumber = "12345678901q"
                )

                val response = client.post("/api/v1/followupplan") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
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
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<FollowUpPlanDTOValidationError>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage.type shouldBe ErrorType.VALIDATION_ERROR
                responseMessage.message shouldBe (
                    "employeeHasNotContributedToPlanDescription is mandatory " +
                        "if employeeHasContributedToPlan = false"
                    )
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
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                val responseMessage = response.body<String>()

                response shouldHaveStatus HttpStatusCode.BadRequest
                responseMessage shouldContain "needsHelpFromNavDescription is obligatory if needsHelpFromNav is true"
            }
        }

        it("Fails when no plan with requested uuid is found") {
            testApplication {
                val (_, client) = configureTestApplication()
                val uuid = UUID.randomUUID()
                val response = client.get("/api/v1/followupplan/$uuid/sendingstatus") {
                    bearerAuth(validMaskinportenToken(consumerOrgnumber = VIRKSOMHETSNUMMER))
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
                responseBody shouldContain "Missing consumer claim in JWT"
            }
        }

        it("Does not fail when supplier is missing") {
            testApplication {
                val (_, client) = configureTestApplication()

                val response = client.get("/api/v1/followupplan/verify-integration") {
                    bearerAuth(customMaskinportenToken(supplierOrgnumber = null))
                    contentType(ContentType.Application.Json)
                }

                response shouldHaveStatus HttpStatusCode.OK
                val responseBody = response.body<String>()
                responseBody shouldContain "Integration is up and running"
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
                responseBody shouldContain "Invalid scope in JWT"
            }
        }

        it("Succeeds when everything is OK") {
            testApplication {
                val (_, client) = configureTestApplication()

                val response = client.get("/api/v1/followupplan/verify-integration") {
                    bearerAuth(validMaskinportenToken())
                    contentType(ContentType.Application.Json)
                }

                response shouldHaveStatus HttpStatusCode.OK
                val responseBody = response.body<String>()
                responseBody shouldBe "Integration is up and running"
            }
        }
    }
})
