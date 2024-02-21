package no.nav.syfo.api.oppfolgingsplanmottak

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.createDefaultOppfolgingsplanDTOMock
import no.nav.syfo.mockdata.testApiModule
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.successText
import no.nav.syfo.util.validMaskinportenToken
import java.time.LocalDate

class OppfolgingsplanApiTest : DescribeSpec({
    val embeddedDatabase = EmbeddedDatabase()

    describe("Retrieval of oppfølgingsplaner") {
        it("Should get a dummy response for POST") {
            testApplication {
                application {
                    testApiModule(ExternalMockEnvironment.instance, embeddedDatabase)
                }
                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerKotlinModule()
                            registerModule(JavaTimeModule())
                            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        }
                    }
                }
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
                application {
                    testApiModule(ExternalMockEnvironment.instance, embeddedDatabase)
                }
                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerKotlinModule()
                            registerModule(JavaTimeModule())
                            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        }
                    }
                }

                val followUpPlanDTO = FollowUpPlanDTO(
                    employeeIdentificationNumber = "123456789",
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

                val response = client.post("/api/v1/followupplan/write") {
                    bearerAuth(validMaskinportenToken())
                    contentType(ContentType.Application.Json)
                    setBody(followUpPlanDTO)
                }
                response shouldHaveStatus HttpStatusCode.OK
            }
        }
    }
})
