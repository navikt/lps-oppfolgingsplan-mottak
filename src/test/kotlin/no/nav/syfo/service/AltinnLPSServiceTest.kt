package no.nav.syfo.service


import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import no.nav.syfo.consumer.isdialogmelding.IsdialogmeldingConsumer
import no.nav.syfo.consumer.oppdfgen.OpPdfGenConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.db.getLpsByUuid
import no.nav.syfo.environment.getEnv
import no.nav.syfo.kafka.producers.NavLpsProducer
import no.nav.syfo.util.LpsHelper
import no.nav.syfo.util.deleteRowsInAltinnLpsTable


class AltinnLPSServiceTest : DescribeSpec({
    val env = getEnv()
    val embeddedDatabase = EmbeddedDatabase()
    val lpsHelper = LpsHelper()
    val opPdfGenConsumer = mockk<OpPdfGenConsumer>()
    val pdlConsumer = mockk<PdlConsumer>()
    val navLpsProducer = mockk<NavLpsProducer>()
    val isdialogmeldingConsumer = mockk<IsdialogmeldingConsumer>()

    val (archiveReference, arbeidstakerFnr, lpsXml) = lpsHelper.receiveLPS()
    val (archiveReference2, arbeidstakerFnr2, lpsXml2) = lpsHelper.receiveLPSWithoutDelingSet()
    val pdfByteArray = "<MOCK PDF CONTENT>".toByteArray()


    val altinnLPSService = AltinnLPSService(
        pdlConsumer,
        opPdfGenConsumer,
        embeddedDatabase,
        navLpsProducer,
        isdialogmeldingConsumer,
        env.altinnLps.sendToGpRetryThreshold,
    )

    beforeTest {
        clearAllMocks()
        embeddedDatabase.deleteRowsInAltinnLpsTable()
        justRun { navLpsProducer.sendAltinnLpsToNav(any()) }
        every { opPdfGenConsumer.generatedPdfResponse(any()) } returns pdfByteArray
        every { isdialogmeldingConsumer.sendPlanToGeneralPractitioner(any(), pdfByteArray) } returns true
        every { pdlConsumer.mostRecentFnr(arbeidstakerFnr) } returns arbeidstakerFnr
        every { pdlConsumer.mostRecentFnr(arbeidstakerFnr2) } returns arbeidstakerFnr2
    }

    afterSpec { embeddedDatabase.stop() }

    describe("Receive Altinn-LPS from altinnkanal-2") {

        it ("Receive LPS with BistandFraNAV and DelMedFastlege set to true") {
            val uuid = altinnLPSService.persistLpsPlan(archiveReference, lpsXml)
            altinnLPSService.processLpsPlan(uuid)

            val storedLps = embeddedDatabase.getLpsByUuid(uuid)
            storedLps.archiveReference shouldBe archiveReference
            storedLps.lpsFnr shouldBe arbeidstakerFnr
            storedLps.fnr shouldBe arbeidstakerFnr
            storedLps.pdf shouldNotBe null
            storedLps.sentToGp shouldBe true

            verify(exactly = 1) {
                isdialogmeldingConsumer.sendPlanToGeneralPractitioner(arbeidstakerFnr, pdfByteArray)
            }
            verify(exactly = 1) {
                navLpsProducer.sendAltinnLpsToNav(any())
            }
        }

        it("Receive LPS with BistandFraNAV and DelMedFastlege set to false") {
            val uuid = altinnLPSService.persistLpsPlan(archiveReference2, lpsXml2)
            altinnLPSService.processLpsPlan(uuid)

            val storedLps = embeddedDatabase.getLpsByUuid(uuid)
            storedLps.archiveReference shouldBe archiveReference2
            storedLps.lpsFnr shouldBe arbeidstakerFnr2
            storedLps.fnr shouldBe arbeidstakerFnr2
            storedLps.pdf shouldNotBe null
            storedLps.sentToGp shouldBe false

            verify(exactly = 0) {
                isdialogmeldingConsumer.sendPlanToGeneralPractitioner(arbeidstakerFnr2, pdfByteArray)
            }
            verify(exactly = 0) {
                navLpsProducer.sendAltinnLpsToNav(any())
            }
        }

        it("Arbeidstaker has FNR different from LPS-form") {
            val currentFnr = arbeidstakerFnr.reversed()
            every { pdlConsumer.mostRecentFnr(arbeidstakerFnr) } returns currentFnr

            val uuid = altinnLPSService.persistLpsPlan(archiveReference, lpsXml)
            altinnLPSService.processLpsPlan(uuid)

            val storedLps = embeddedDatabase.getLpsByUuid(uuid)
            storedLps.fnr shouldNotBe null
            storedLps.lpsFnr shouldNotBe storedLps.fnr
        }

        it("LPS plan is scheduled for retry when either FNR-fetching or PDF-generation fails") {
            every { pdlConsumer.mostRecentFnr(arbeidstakerFnr) } returns null
            every { opPdfGenConsumer.generatedPdfResponse(any()) } returns null

            val uuid = altinnLPSService.persistLpsPlan(archiveReference, lpsXml)
            altinnLPSService.processLpsPlan(uuid)

            val uuid2 = altinnLPSService.persistLpsPlan(archiveReference2, lpsXml2)
            altinnLPSService.processLpsPlan(uuid2)

            val storedLps = embeddedDatabase.getLpsByUuid(uuid)
            storedLps.archiveReference shouldBe archiveReference
            storedLps.lpsFnr shouldBe arbeidstakerFnr
            storedLps.fnr shouldBe null

            val storedLps2 = embeddedDatabase.getLpsByUuid(uuid2)
            storedLps2.archiveReference shouldBe archiveReference2
            storedLps2.lpsFnr shouldBe arbeidstakerFnr2
            storedLps2.fnr shouldNotBe null
            storedLps2.pdf shouldBe null
        }
    }
})
