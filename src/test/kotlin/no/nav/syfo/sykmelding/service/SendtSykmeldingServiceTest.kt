package no.nav.syfo.sykmelding.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import no.nav.syfo.db.EmbeddedDatabase
import no.nav.syfo.sykmelding.domain.SykmeldingsperiodeAGDTO
import no.nav.syfo.util.deleteData
import java.time.LocalDate

class SendtSykmeldingServiceTest : DescribeSpec({
    val embeddedDatabase = EmbeddedDatabase()

    val sendtSykmeldingService = SendtSykmeldingService(embeddedDatabase)

    beforeTest {
        clearAllMocks()
        embeddedDatabase.deleteData()
    }

    describe("Sykmeldingperioder") {
        it("Should persist sykmeldingperioder") {
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

            sendtSykmeldingService.persistSykmeldingperioder(
                sykmeldingId = sykmeldingId,
                orgnumber = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                sykmeldingsperioder = sykmeldingsperioder
            )

            val storedSykmeldingsperioder =
                sendtSykmeldingService.getSykmeldingperioder(orgnumber, employeeIdentificationNumber)

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

            sendtSykmeldingService.persistSykmeldingperioder(
                sykmeldingId = sykmeldingId,
                orgnumber = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                sykmeldingsperioder = sykmeldingsperioder
            )

            val storedSykmeldingsperioder =
                sendtSykmeldingService.getSykmeldingperioder(orgnumber, employeeIdentificationNumber)

            storedSykmeldingsperioder.size shouldBe 2

            sendtSykmeldingService.deleteSykmeldingperioder(sykmeldingId)

            val storedSykmeldingsperioderAfterDelete =
                sendtSykmeldingService.getSykmeldingperioder(orgnumber, employeeIdentificationNumber)

            storedSykmeldingsperioderAfterDelete.size shouldBe 0
        }
    }
})
