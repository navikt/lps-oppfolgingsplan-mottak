package no.nav.syfo.altinnmottak

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.altinnmottak.database.getAltinnLpsOppfolgingsplanByUuid
import no.nav.syfo.altinnmottak.kafka.AltinnOppfolgingsplanProducer
import no.nav.syfo.application.environment.getEnv
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.util.LpsHelper

class AltinnLpsServiceTest : DescribeSpec({
    val env = getEnv()
    val embeddedDatabase = EmbeddedDatabase()
    val lpsHelper = LpsHelper()
    val opPdfGenClient = mockk<OpPdfGenClient>()
    val pdlClient = mockk<PdlClient>()
    val navLpsProducer = mockk<AltinnOppfolgingsplanProducer>()
    val isdialogmeldingClient = mockk<IsdialogmeldingClient>()
    val dokarkivClient = mockk<DokarkivClient>()

    val (archiveReference, arbeidstakerFnr, lpsXml) = lpsHelper.receiveLps()
    val (archiveReference2, arbeidstakerFnr2, lpsXml2) = lpsHelper.receiveLpsWithoutDelingSet()
    val pdfByteArray = "<MOCK PDF CONTENT>".toByteArray()

    val altinnLpsService = AltinnLpsService(
        pdlClient,
        opPdfGenClient,
        embeddedDatabase,
        navLpsProducer,
        isdialogmeldingClient,
        dokarkivClient,
        env.altinnLps.sendToFastlegeRetryThreshold,
        env.toggles,
    )

    beforeTest {
        clearAllMocks()
        embeddedDatabase.dropData()
        justRun { navLpsProducer.sendAltinnLpsToNav(any()) }
        coEvery { opPdfGenClient.generatedPdfResponse(any()) } returns pdfByteArray
        coEvery { isdialogmeldingClient.sendLpsPlanToGeneralPractitioner(any(), pdfByteArray) } returns true
        coEvery { pdlClient.mostRecentFnr(arbeidstakerFnr) } returns arbeidstakerFnr
        coEvery { pdlClient.mostRecentFnr(arbeidstakerFnr2) } returns arbeidstakerFnr2
    }

    describe("Receive Altinn-LPS from altinnkanal-2") {

        it("Receive LPS with BistandFraNAV and DelMedFastlege set to true") {
            val uuid = altinnLpsService.persistLpsPlan(archiveReference, lpsXml)
            altinnLpsService.processLpsPlan(uuid)

            val storedLps = embeddedDatabase.getAltinnLpsOppfolgingsplanByUuid(uuid)
            storedLps.archiveReference shouldBe archiveReference
            storedLps.lpsFnr shouldBe arbeidstakerFnr
            storedLps.fnr shouldBe arbeidstakerFnr
            storedLps.pdf shouldNotBe null
            storedLps.sentToFastlege shouldBe true
            storedLps.sentToNav shouldBe true

            coVerify(exactly = 1) {
                isdialogmeldingClient.sendLpsPlanToGeneralPractitioner(arbeidstakerFnr, pdfByteArray)
            }
            verify(exactly = 1) {
                navLpsProducer.sendAltinnLpsToNav(any())
            }
        }

        it("Receive LPS with BistandFraNAV and DelMedFastlege set to false") {
            val uuid = altinnLpsService.persistLpsPlan(archiveReference2, lpsXml2)
            altinnLpsService.processLpsPlan(uuid)

            val storedLps = embeddedDatabase.getAltinnLpsOppfolgingsplanByUuid(uuid)
            storedLps.archiveReference shouldBe archiveReference2
            storedLps.lpsFnr shouldBe arbeidstakerFnr2
            storedLps.fnr shouldBe arbeidstakerFnr2
            storedLps.pdf shouldNotBe null
            storedLps.sentToFastlege shouldBe false
            storedLps.sentToNav shouldBe false

            coVerify(exactly = 0) {
                isdialogmeldingClient.sendLpsPlanToGeneralPractitioner(arbeidstakerFnr2, pdfByteArray)
            }
            verify(exactly = 0) {
                navLpsProducer.sendAltinnLpsToNav(any())
            }
        }

        it("Arbeidstaker has FNR different from LPS-form") {
            val currentFnr = arbeidstakerFnr.reversed()
            coEvery { pdlClient.mostRecentFnr(arbeidstakerFnr) } returns currentFnr

            val uuid = altinnLpsService.persistLpsPlan(archiveReference, lpsXml)
            altinnLpsService.processLpsPlan(uuid)

            val storedLps = embeddedDatabase.getAltinnLpsOppfolgingsplanByUuid(uuid)
            storedLps.fnr shouldNotBe null
            storedLps.lpsFnr shouldNotBe storedLps.fnr
        }

        it("LPS plan is scheduled for retry when either FNR-fetching or PDF-generation fails") {
            coEvery { pdlClient.mostRecentFnr(arbeidstakerFnr) } returns null
            coEvery { opPdfGenClient.generatedPdfResponse(any()) } returns null

            val uuid = altinnLpsService.persistLpsPlan(archiveReference, lpsXml)
            altinnLpsService.processLpsPlan(uuid)

            val uuid2 = altinnLpsService.persistLpsPlan(archiveReference2, lpsXml2)
            altinnLpsService.processLpsPlan(uuid2)

            val storedLps = embeddedDatabase.getAltinnLpsOppfolgingsplanByUuid(uuid)
            storedLps.archiveReference shouldBe archiveReference
            storedLps.lpsFnr shouldBe arbeidstakerFnr
            storedLps.fnr shouldBe null

            val storedLps2 = embeddedDatabase.getAltinnLpsOppfolgingsplanByUuid(uuid2)
            storedLps2.archiveReference shouldBe archiveReference2
            storedLps2.lpsFnr shouldBe arbeidstakerFnr2
            storedLps2.fnr shouldNotBe null
            storedLps2.pdf shouldBe null
        }
    }
})
