package no.nav.syfo.oppfolgingsplanmottak.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.mockdata.randomFollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import java.util.*

class FollowUpPlanSendingServiceTest : DescribeSpec({
    val isdialogmeldingClient = mockk<IsdialogmeldingClient>(relaxed = true)
    val followupPlanProducer = mockk<FollowUpPlanProducer>(relaxed = true)
    val opPdfGenClient = mockk<OpPdfGenClient>(relaxed = true)
    val dokarkivClient = mockk<DokarkivClient>(relaxed = true)
    val service =
        FollowUpPlanSendingService(isdialogmeldingClient, followupPlanProducer, opPdfGenClient, dokarkivClient, false)
    val pdfByteArray = "<MOCK PDF CONTENT>".toByteArray()

    beforeSpec {
        coEvery { opPdfGenClient.getLpsPdf(any()) } returns pdfByteArray
        coEvery { dokarkivClient.journalforLps(any(), any(), any(), any()) } returns "id123"
    }

    describe("FollowUpPlanSendingService") {
        it(
            "sends plan to gp when sendPlanToGeneralPractitioner is true and sendLpsPlanToFastlegeToggle is enabled",
        ) {
            runBlocking {
                val followUpPlanDTO = randomFollowUpPlanDTO.copy(sendPlanToGeneralPractitioner = true)
                val uuid = UUID.randomUUID()
                val employerOrgnr = "987654321"

                val response = service.sendFollowUpPlan(followUpPlanDTO, uuid, employerOrgnr)

                coVerify(exactly = 1) { isdialogmeldingClient.sendLpsPlanToGeneralPractitioner(any(), any()) }
                response.isSentToGeneralPractitionerStatus shouldBe true
            }
        }

        it("sent-status is true when sendPlanToNav is true") {
            runBlocking {
                val followUpPlanDTO =
                    randomFollowUpPlanDTO.copy(
                        sendPlanToNav = true,
                    )
                val uuid = UUID.randomUUID()
                val employerOrgnr = "987654321"

                val response = service.sendFollowUpPlan(followUpPlanDTO, uuid, employerOrgnr)

                verify(exactly = 0) { followupPlanProducer.createFollowUpPlanTaskInModia(any()) }
                response.isSentToNavStatus shouldBe true
            }
        }

        it("sent-status is false, and does create task in Modia when sendPlanToNav is false") {
            runBlocking {
                val followUpPlanDTO =
                    randomFollowUpPlanDTO.copy(
                        sendPlanToNav = false,
                    )
                val uuid = UUID.randomUUID()
                val employerOrgnr = "987654321"

                val response = service.sendFollowUpPlan(followUpPlanDTO, uuid, employerOrgnr)

                verify(exactly = 0) { followupPlanProducer.createFollowUpPlanTaskInModia(any()) }
                response.isSentToNavStatus shouldBe false
            }
        }

        it("creates task in Modia when sendPlanToNav is true and needsHelpFromNav is true") {
            runBlocking {
                val followUpPlanDTO =
                    randomFollowUpPlanDTO.copy(
                        sendPlanToNav = true,
                        needsHelpFromNav = true,
                        needsHelpFromNavDescription = "Needs help from NAV description",
                    )
                val uuid = UUID.randomUUID()
                val employerOrgnr = "987654321"

                val response = service.sendFollowUpPlan(followUpPlanDTO, uuid, employerOrgnr)

                verify(exactly = 1) { followupPlanProducer.createFollowUpPlanTaskInModia(any()) }
                response.isSentToNavStatus shouldBe true
            }
        }
    }
})
