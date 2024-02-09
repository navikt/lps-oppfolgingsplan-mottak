package no.nav.syfo.veileder

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.UserConstants
import no.nav.syfo.mockdata.testApiModule
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.validVeilederToken

class VeilederApiTest : DescribeSpec({
    val embeddedDatabase = EmbeddedDatabase()

    afterSpec { embeddedDatabase.stop() }

    describe("Happy path") {
        it("Returns OK with valid token") {
            testApplication {
                application {
                    testApiModule(ExternalMockEnvironment.instance, embeddedDatabase)
                }

                val response = client.get(VEILEDER_LPS_METADATA_PATH) {
                    bearerAuth(validVeilederToken())
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                }
                response shouldHaveStatus HttpStatusCode.OK
            }
        }
    }

    describe("Unhappy path") {
        it("Returns status Unauthorized if no token is supplied") {
            testApplication {
                application {
                    testApiModule(ExternalMockEnvironment.instance, embeddedDatabase)
                }
                val response = client.get(VEILEDER_LPS_METADATA_PATH)
                response shouldHaveStatus HttpStatusCode.Unauthorized
            }
        }
    }
})
