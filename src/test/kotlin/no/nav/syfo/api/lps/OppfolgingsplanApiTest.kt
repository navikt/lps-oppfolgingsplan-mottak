package no.nav.syfo.api.lps

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
import no.nav.syfo.mockdata.createDefaultOppfolgingsplanDTOMock
import no.nav.syfo.environment.getEnv
import no.nav.syfo.serverModule

class OppfolgingsplanApiTest : DescribeSpec({

    describe("Retrieval of oppfølgingsplaner") {
        it("Should get a dummy response for POST") {
            testApplication {
                application {
                    serverModule(getEnv())
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
                val response = client.post("/api/v1/lps/write")
                {
                    contentType(ContentType.Application.Json)
                    setBody(createDefaultOppfolgingsplanDTOMock())
                }
                response shouldHaveStatus HttpStatusCode.OK
                response.bodyAsText() shouldContain "Recieved oppfolgingsplan for virksomhet Ørsta Rådhus"
            }
        }
    }

})
