package no.nav.syfo.api.oppfolgingsplanmottak

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.application.environment.getEnv
import no.nav.syfo.client.isdialogmelding.DelingMedFastlegeStatusResponse
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.createDefaultOppfolgingsplanDTOMock
import no.nav.syfo.mockdata.testApiModule
import no.nav.syfo.oppfolgingsplanmottak.successText
import no.nav.syfo.util.validMaskinportenToken

class OppfolgingsplanApiTest : DescribeSpec({
    val env = getEnv()
    val embeddedDatabase = EmbeddedDatabase()
    val isdialogmeldingConsumer = mockk<IsdialogmeldingClient>(relaxed = true)

    val sentToFastlegeId = "sentToFastlegeId"

    beforeTest {
        clearAllMocks()
        coEvery { isdialogmeldingConsumer.sendLpsPlanToFastlege(any(), any()) } returns sentToFastlegeId
        coEvery { isdialogmeldingConsumer.getDeltMedFastlegeStatus(any()) } returns DelingMedFastlegeStatusResponse(sentToFastlegeId, true)
    }
    afterSpec { embeddedDatabase.stop() }

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
    }
})
