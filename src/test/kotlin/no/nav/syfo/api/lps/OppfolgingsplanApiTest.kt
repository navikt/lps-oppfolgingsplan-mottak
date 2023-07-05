package no.nav.syfo.api.lps

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.environment.getEnv
import no.nav.syfo.serverModule

class OppfolgingsplanApiTest : DescribeSpec({

    describe("Retrieval of oppfølgingsplaner") {
        it("Should get a dummy response for GET") {
            testApplication {
                application {
                    serverModule(getEnv())
                }
                val response = client.get("/api/v1/lps/write")
                response shouldHaveStatus HttpStatusCode.OK
                response.bodyAsText() shouldContain "Called GET /api/v1/lps/write"
            }
        }

        it("Should get a dummy response for POST") {
            testApplication {
                application {
                    serverModule(getEnv())
                }
                val response = client.post("/api/v1/lps/write")
                response shouldHaveStatus HttpStatusCode.OK
                response.bodyAsText() shouldContain "Called POST /api/v1/lps/write"
            }
        }
    }
})
