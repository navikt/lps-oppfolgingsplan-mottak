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
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.environment.getEnv
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.createDefaultOppfolgingsplanDTOMock
import no.nav.syfo.oppfolgingsplanmottak.successText

class OppfolgingsplanApiTest : DescribeSpec({
    val embeddedDatabase = EmbeddedDatabase()

    afterSpec { embeddedDatabase.stop() }

    describe("Retrieval of oppfølgingsplaner") {
        it("Should get a dummy response for POST") {
            testApplication {
                application {
                    apiModule(ApplicationState(alive = true, ready = true), embeddedDatabase, getEnv())
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
                val response = client.post("/api/v1/lps/write")
                {
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
