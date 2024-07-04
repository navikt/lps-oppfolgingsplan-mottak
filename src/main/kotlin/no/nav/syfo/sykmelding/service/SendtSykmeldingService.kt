package no.nav.syfo.sykmelding.service

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.sykmelding.database.deleteSykmeldingperioder
import no.nav.syfo.sykmelding.database.getSykmeldingperioder
import no.nav.syfo.sykmelding.database.hasActiveSentSykmelding
import no.nav.syfo.sykmelding.database.persistSykmeldingperioder
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.domain.SykmeldingsperiodeAGDTO

class SendtSykmeldingService(private val database: DatabaseInterface) {
    fun persistSykmeldingperioder(
        sykmeldingId: String,
        orgnumber: String,
        employeeIdentificationNumber: String,
        sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>
    ) {
        database.persistSykmeldingperioder(
            sykmeldingId = sykmeldingId,
            orgnumber = orgnumber,
            employeeIdentificationNumber = employeeIdentificationNumber,
            sykmeldingsperioder = sykmeldingsperioder
        )
    }

    fun deleteSykmeldingperioder(sykmeldingId: String) {
        database.deleteSykmeldingperioder(sykmeldingId)
    }

    fun getSykmeldingperioder(
        orgnumber: String,
        employeeIdentificationNumber: String,
    ): List<Sykmeldingsperiode> {
        return database.getSykmeldingperioder(orgnumber, employeeIdentificationNumber)
    }

    fun hasActiveSentSykmelding(
        orgnumber: String,
        employeeIdentificationNumber: String,
    ): Boolean {
        return database.hasActiveSentSykmelding(
            orgnumber,
            employeeIdentificationNumber
        )
    }
}
