package no.nav.syfo.sykmelding.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.sykmelding.domain.SykmeldingsperiodeAGDTO
import java.time.LocalDate

class SendtSykmeldingServiceTest : DescribeSpec({
    val embeddedDatabase = EmbeddedDatabase()

    val sendtSykmeldingService = SendtSykmeldingService(embeddedDatabase)

    beforeTest {
        clearAllMocks()
        embeddedDatabase.dropData()
    }

    describe("Sykmeldingsperioder") {
        it("Should persist sykmeldingsperioder") {
            val sykmeldingId = "123"
            val orgnumber = "456"
            val employeeIdentificationNumber = "789"
            val sykmeldingsperioder = listOf(
                SykmeldingsperiodeAGDTO(
                    fom = LocalDate.now().minusWeeks(10),
                    tom = LocalDate.now().plusDays(10),
                ),
                SykmeldingsperiodeAGDTO(
                    fom = LocalDate.now().minusWeeks(20),
                    tom = LocalDate.now().minusWeeks(17),
                )
            )

            sendtSykmeldingService.persistSykmeldingsperioder(
                sykmeldingId = sykmeldingId,
                orgnumber = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                sykmeldingsperioder = sykmeldingsperioder
            )

            val storedSykmeldingsperioder =
                sendtSykmeldingService.getSykmeldingsperioder(orgnumber, employeeIdentificationNumber)

            storedSykmeldingsperioder.size shouldBe 2
        }

        it("Should delete tombstone records") {
            val sykmeldingId = "123"
            val orgnumber = "456"
            val employeeIdentificationNumber = "789"
            val sykmeldingsperioder = listOf(
                SykmeldingsperiodeAGDTO(
                    fom = LocalDate.now().minusWeeks(10),
                    tom = LocalDate.now().plusDays(10),
                ),
                SykmeldingsperiodeAGDTO(
                    fom = LocalDate.now().minusWeeks(20),
                    tom = LocalDate.now().minusWeeks(17),
                )
            )

            sendtSykmeldingService.persistSykmeldingsperioder(
                sykmeldingId = sykmeldingId,
                orgnumber = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                sykmeldingsperioder = sykmeldingsperioder
            )

            val storedSykmeldingsperioder =
                sendtSykmeldingService.getSykmeldingsperioder(orgnumber, employeeIdentificationNumber)

            storedSykmeldingsperioder.size shouldBe 2

            sendtSykmeldingService.deleteSykmeldingsperioder(sykmeldingId)

            val storedSykmeldingsperioderAfterDelete =
                sendtSykmeldingService.getSykmeldingsperioder(orgnumber, employeeIdentificationNumber)

            storedSykmeldingsperioderAfterDelete.size shouldBe 0
        }

        it("Should return true for active sendt sykmelding if period exists between fom and tom") {
            val sykmeldingId = "123"
            val orgnumber = "456"
            val employeeIdentificationNumber = "789"
            val sykmeldingsperioder = listOf(
                SykmeldingsperiodeAGDTO(
                    fom = LocalDate.now().minusWeeks(10),
                    tom = LocalDate.now().plusWeeks(10),
                ),
            )

            sendtSykmeldingService.persistSykmeldingsperioder(
                sykmeldingId = sykmeldingId,
                orgnumber = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                sykmeldingsperioder = sykmeldingsperioder
            )

            val hasActiveSentSykmelding =
                sendtSykmeldingService.hasActiveSentSykmelding(orgnumber, employeeIdentificationNumber)

            hasActiveSentSykmelding shouldBe true
        }

        it("Should return false for active sendt sykmelding if period does not exist between fom and tom") {
            val sykmeldingId = "123"
            val orgnumber = "456"
            val employeeIdentificationNumber = "789"
            val sykmeldingsperioder = listOf(
                SykmeldingsperiodeAGDTO(
                    fom = LocalDate.now().minusWeeks(10),
                    tom = LocalDate.now().minusWeeks(4),
                ),
            )

            sendtSykmeldingService.persistSykmeldingsperioder(
                sykmeldingId = sykmeldingId,
                orgnumber = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                sykmeldingsperioder = sykmeldingsperioder
            )

            val hasActiveSentSykmelding =
                sendtSykmeldingService.hasActiveSentSykmelding(orgnumber, employeeIdentificationNumber)

            hasActiveSentSykmelding shouldBe false
        }
    }
})
