package no.nav.syfo.api.oppfolgingsplanmottak

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.syfo.application.environment.getEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.DelingMedFastlegeStatusResponse
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.database
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.createDefaultOppfolgingsplanDTOMock
import no.nav.syfo.mockdata.testApiModule
import no.nav.syfo.service.LpsOppfolgingsplan
import no.nav.syfo.util.validMaskinportenToken

class OppfolgingsplanApiTest : DescribeSpec({
    val env = getEnv()
    val embeddedDatabase = EmbeddedDatabase()
    val isdialogmeldingConsumer = mockk<IsdialogmeldingClient>(relaxed = true)
    val opPdfGenConsumer = mockk<OpPdfGenClient>(relaxed = true)
    val dokarkivConsumer = mockk<DokarkivClient>(relaxed = true)

    val sentToFastlegeId = "sentToFastlegeId"

    beforeTest {
        clearAllMocks()
        coEvery { isdialogmeldingConsumer.sendLpsPlanToFastlege(any(), any()) } returns sentToFastlegeId
        coEvery { isdialogmeldingConsumer.getDeltMedFastlegeStatus(any()) } returns DelingMedFastlegeStatusResponse(sentToFastlegeId, true)
    }
    afterSpec { embeddedDatabase.stop() }

    describe("Retrieval of oppfølgingsplaner") {
        it("Should get a HttpStatusCode.OK response with LPS plan object with expected sentToFastlegeId") {
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
                val responseBody = response.body<LpsOppfolgingsplan>()
                response shouldHaveStatus HttpStatusCode.OK
                responseBody shouldNotBe null
                responseBody.sentToFastlegeId shouldBe sentToFastlegeId

                coVerify(exactly = 0) {
                    isdialogmeldingConsumer.sendAltinnLpsPlanToFastlege(any(), any())
                }
                coVerify(exactly = 1) {
                    isdialogmeldingConsumer.sendLpsPlanToFastlege(any(), any())
                }
            }
        }
    }

    it("Should get a HttpStatusCode.OK response with expected sharing status and id") {
        testApplication {
            application {
                testApiModule(ExternalMockEnvironment.instance, embeddedDatabase)
            }
            database = embeddedDatabase
            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    }
                }
            }

            val response = client.get("/api/v1/lps/status/delt/fastlege") {
                contentType(ContentType.Application.Json)
                setAttributes { parameter("sentToFastlegeId", "123") }
            }
            val responseBody = response.body<DelingMedFastlegeStatusResponse>()

            response shouldHaveStatus HttpStatusCode.OK
            responseBody shouldNotBe null
            responseBody.sendingToFastlegeId shouldBe sentToFastlegeId
            responseBody.isSent shouldBe true

            coVerify(exactly = 1) {
                isdialogmeldingConsumer.getDeltMedFastlegeStatus("123")
            }
        }
    }
})
