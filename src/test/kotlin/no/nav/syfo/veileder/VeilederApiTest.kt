package no.nav.syfo.veileder

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.shouldForAtLeastOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.altinnmottak.database.storeAltinnLpsOppfolgingsplan
import no.nav.syfo.altinnmottak.database.storePdf
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.UserConstants
import no.nav.syfo.mockdata.testApiModule
import no.nav.syfo.oppfolgingsplanmottak.database.storeFollowUpPlan
import no.nav.syfo.oppfolgingsplanmottak.database.storeLpsPdf
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.configure
import no.nav.syfo.util.deleteData
import no.nav.syfo.util.validVeilederToken
import no.nav.syfo.veileder.domain.OppfolgingsplanLPS
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

val altinnLpsPlan = AltinnLpsOppfolgingsplan(
    archiveReference = "archiveReferance",
    uuid = UUID.randomUUID(),
    lpsFnr = UserConstants.ARBEIDSTAKER_FNR,
    fnr = null,
    orgnummer = UserConstants.VIRKSOMHETSNUMMER,
    pdf = null,
    xml = "<xml>",
    shouldSendToNav = true,
    shouldSendToFastlege = true,
    sentToNav = false,
    sentToFastlege = false,
    sendToFastlegeRetryCount = 0,
    journalpostId = null,
    originallyCreated = LocalDateTime.now(),
    created = LocalDateTime.now(),
    lastChanged = LocalDateTime.now()
)

val oppfolgingsplanUUID = UUID.randomUUID()

val oppfolgingsplan = FollowUpPlanDTO(
    UserConstants.ARBEIDSTAKER_FNR,
    typicalWorkday = "Typisk arbeidsdag",
    tasksThatCanStillBeDone = "Programmere",
    tasksThatCanNotBeDone = "Lage PowerPoint",
    previousFacilitation = "Har ikke prøvd noe",
    plannedFacilitation = "Skal prøve noe",
    otherFacilitationOptions = "Ikke noe",
    followUp = "Jada, skal følge opp",
    evaluationDate = LocalDate.now().plusDays(20),
    sendPlanToNav = true,
    needsHelpFromNav = null,
    needsHelpFromNavDescription = null,
    sendPlanToGeneralPractitioner = true,
    messageToGeneralPractitioner = "Hei Lege",
    additionalInformation = "Litt tilleggsinfo",
    contactPersonFullName = "Nissefar",
    contactPersonPhoneNumber = "99887766",
    employeeHasContributedToPlan = true,
    employeeHasNotContributedToPlanDescription = null,
    lpsName = "TEST"
)

class VeilederApiTest : DescribeSpec({
    val embeddedDatabase = EmbeddedDatabase()

    beforeTest {
        clearAllMocks()
        embeddedDatabase.deleteData()
    }

    fun testConfiguredApplication(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                jackson { configure() }
            }
        }
        application {
            testApiModule(ExternalMockEnvironment.instance, embeddedDatabase)
        }
        block(client)
    }

    describe("Get metadata for LPS plans for Veileder") {

        it("Happy path") {
            testConfiguredApplication {
                embeddedDatabase.storeAltinnLpsOppfolgingsplan(altinnLpsPlan)
                embeddedDatabase.storePdf(altinnLpsPlan.uuid, byteArrayOf(0x2E, 0x38))
                embeddedDatabase.storeFollowUpPlan(
                    oppfolgingsplanUUID,
                    oppfolgingsplan,
                    UserConstants.VIRKSOMHETSNUMMER,
                    UserConstants.LPS_VIRKSOMHETSNUMMER
                )
                embeddedDatabase.storeLpsPdf(oppfolgingsplanUUID, byteArrayOf(0x2E, 0x38))

                val response = it.get(VEILEDER_LPS_BASE_PATH) {
                    bearerAuth(validVeilederToken())
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                }
                response shouldHaveStatus HttpStatusCode.OK
                val oppfolgingsplaner = response.body<List<OppfolgingsplanLPS>>()
                oppfolgingsplaner.size shouldBeExactly 2

                oppfolgingsplaner.shouldForAtLeastOne { plan ->
                    plan.uuid shouldBeEqual altinnLpsPlan.uuid
                    plan.fnr shouldBeEqual altinnLpsPlan.lpsFnr
                    plan.virksomhetsnummer shouldBeEqual altinnLpsPlan.orgnummer
                }

                oppfolgingsplaner.shouldForAtLeastOne { plan ->
                    plan.uuid shouldBeEqual oppfolgingsplanUUID
                    plan.fnr shouldBeEqual oppfolgingsplan.employeeIdentificationNumber
                    plan.virksomhetsnummer shouldBeEqual UserConstants.VIRKSOMHETSNUMMER
                }
            }
        }

        it("Returns status Unauthorized if no token is supplied") {
            testConfiguredApplication {
                val response = it.get(VEILEDER_LPS_BASE_PATH)
                response shouldHaveStatus HttpStatusCode.Unauthorized
            }
        }

        it("Returns status Forbidden if veileder hasn't got access") {
            testConfiguredApplication {

                val response = it.get(VEILEDER_LPS_BASE_PATH) {
                    bearerAuth(validVeilederToken())
                    header(NAV_PERSONIDENT_HEADER, UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value)
                }
                response shouldHaveStatus HttpStatusCode.Forbidden
            }
        }

        it("Returns empty list if plan isn't shared with NAV") {
            testConfiguredApplication {
                embeddedDatabase.storeAltinnLpsOppfolgingsplan(altinnLpsPlan.copy(shouldSendToNav = false))
                embeddedDatabase.storePdf(altinnLpsPlan.uuid, byteArrayOf(0x2E, 0x38))
                embeddedDatabase.storeFollowUpPlan(
                    oppfolgingsplanUUID,
                    oppfolgingsplan.copy(sendPlanToNav = false),
                    UserConstants.VIRKSOMHETSNUMMER,
                    UserConstants.LPS_VIRKSOMHETSNUMMER
                )
                embeddedDatabase.storeLpsPdf(oppfolgingsplanUUID, byteArrayOf(0x2E, 0x38))

                val response = it.get(VEILEDER_LPS_BASE_PATH) {
                    bearerAuth(validVeilederToken())
                    header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_PERSONIDENT.value)
                }
                response shouldHaveStatus HttpStatusCode.OK
                val oppfolgingsplaner = response.body<List<OppfolgingsplanLPS>>()
                oppfolgingsplaner.size shouldBeExactly 0
            }
        }
    }

    describe("Get pdf for LPS plans for Veileder") {

        it("Returns OK when getting altinn pdf") {
            testConfiguredApplication {

                embeddedDatabase.storeAltinnLpsOppfolgingsplan(altinnLpsPlan)
                val pdfBytes = byteArrayOf(0x2E, 0x38)
                embeddedDatabase.storePdf(altinnLpsPlan.uuid, pdfBytes)

                val response = it.get("$VEILEDER_LPS_BASE_PATH/${altinnLpsPlan.uuid}") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.OK
                val pdf = response.body<ByteArray>()
                pdf shouldBe pdfBytes
            }
        }

        it("Returns OK when getting pdf") {
            testConfiguredApplication {
                val pdfBytes = byteArrayOf(0x2E, 0x38)
                embeddedDatabase.storeFollowUpPlan(
                    oppfolgingsplanUUID,
                    oppfolgingsplan,
                    UserConstants.VIRKSOMHETSNUMMER,
                    UserConstants.LPS_VIRKSOMHETSNUMMER
                )
                embeddedDatabase.storeLpsPdf(oppfolgingsplanUUID, byteArrayOf(0x2E, 0x38))

                val response = it.get("$VEILEDER_LPS_BASE_PATH/$oppfolgingsplanUUID") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.OK
                val pdf = response.body<ByteArray>()
                pdf shouldBe pdfBytes
            }
        }

        it("Returns Forbidden when veileder hasn't got access") {
            testConfiguredApplication {

                embeddedDatabase.storeAltinnLpsOppfolgingsplan(
                    altinnLpsPlan.copy(lpsFnr = UserConstants.PERSONIDENT_VEILEDER_NO_ACCESS.value)
                )
                val pdfBytes = byteArrayOf(0x2E, 0x38)
                embeddedDatabase.storePdf(altinnLpsPlan.uuid, pdfBytes)

                val response = it.get("$VEILEDER_LPS_BASE_PATH/${altinnLpsPlan.uuid}") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.Forbidden
            }
        }

        it("Returns status Bad Request when altinn plan isn't shared with NAV") {
            testConfiguredApplication {

                embeddedDatabase.storeAltinnLpsOppfolgingsplan(altinnLpsPlan.copy(shouldSendToNav = false))
                val pdfBytes = byteArrayOf(0x2E, 0x38)
                embeddedDatabase.storePdf(altinnLpsPlan.uuid, pdfBytes)

                val response = it.get("$VEILEDER_LPS_BASE_PATH/${altinnLpsPlan.uuid}") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.BadRequest
            }
        }

        it("Returns status Bad Request when plan isn't shared with NAV") {
            testConfiguredApplication {
                embeddedDatabase.storeFollowUpPlan(
                    oppfolgingsplanUUID,
                    oppfolgingsplan,
                    UserConstants.VIRKSOMHETSNUMMER,
                    UserConstants.LPS_VIRKSOMHETSNUMMER
                )

                val response = it.get("$VEILEDER_LPS_BASE_PATH/$oppfolgingsplanUUID") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.BadRequest
            }
        }

        it("Returns status Bad Request when invalid UUID") {
            testConfiguredApplication {

                embeddedDatabase.storeAltinnLpsOppfolgingsplan(altinnLpsPlan)
                val pdfBytes = byteArrayOf(0x2E, 0x38)
                embeddedDatabase.storePdf(altinnLpsPlan.uuid, pdfBytes)

                val response = it.get("$VEILEDER_LPS_BASE_PATH/${UUID.randomUUID()}") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.BadRequest
            }
        }

        it("Returns status Bad Request when plan with no pdf") {
            testConfiguredApplication {

                embeddedDatabase.storeAltinnLpsOppfolgingsplan(altinnLpsPlan)

                val response = it.get("$VEILEDER_LPS_BASE_PATH/${altinnLpsPlan.uuid}") {
                    bearerAuth(validVeilederToken())
                }
                response shouldHaveStatus HttpStatusCode.BadRequest
            }
        }
    }
})
