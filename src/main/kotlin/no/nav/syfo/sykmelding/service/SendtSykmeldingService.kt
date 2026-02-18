package no.nav.syfo.sykmelding.service

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.sykmelding.database.deleteSykmeldingsperioder
import no.nav.syfo.sykmelding.database.getActiveSendtSykmeldingsperioder
import no.nav.syfo.sykmelding.database.getSykmeldingsperioder
import no.nav.syfo.sykmelding.database.persistSykmeldingsperiode
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.domain.SykmeldingsperiodeAGDTO
import java.time.LocalDate

class SendtSykmeldingService(
    private val database: DatabaseInterface,
) {
    fun persistSykmeldingsperioder(
        sykmeldingId: String,
        orgnumber: String,
        employeeIdentificationNumber: String,
        sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>,
    ) {
        val activeSykmeldingsPerioder =
            sykmeldingsperioder.filter {
                !it.tom.isBefore(
                    LocalDate.now(),
                )
            }
        activeSykmeldingsPerioder.forEach { sykmeldingsperiode ->
            database.persistSykmeldingsperiode(
                sykmeldingId = sykmeldingId,
                orgnummer = orgnumber,
                employeeIdentificationNumber = employeeIdentificationNumber,
                fom = sykmeldingsperiode.fom,
                tom = sykmeldingsperiode.tom,
            )
        }
    }

    fun deleteSykmeldingsperioder(sykmeldingId: String) {
        database.deleteSykmeldingsperioder(sykmeldingId)
    }

    fun getSykmeldingsperioder(
        orgnumber: String,
        employeeIdentificationNumber: String,
    ): List<Sykmeldingsperiode> = database.getSykmeldingsperioder(orgnumber, employeeIdentificationNumber)

    fun getActiveSendtSykmeldingsperioder(employeeIdentificationNumber: String): List<Sykmeldingsperiode> =
        database.getActiveSendtSykmeldingsperioder(employeeIdentificationNumber)
}
