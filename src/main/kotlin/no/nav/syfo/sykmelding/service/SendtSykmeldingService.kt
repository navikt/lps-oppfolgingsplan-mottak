package no.nav.syfo.sykmelding.service

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.sykmelding.database.deleteSykmeldingsperioder
import no.nav.syfo.sykmelding.database.getSykmeldingsperioder
import no.nav.syfo.sykmelding.database.hasActiveSentSykmelding
import no.nav.syfo.sykmelding.database.persistSykmeldingsperioder
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.domain.SykmeldingsperiodeAGDTO

class SendtSykmeldingService(private val database: DatabaseInterface) {
    fun persistSykmeldingsperioder(
        sykmeldingId: String,
        orgnumber: String,
        employeeIdentificationNumber: String,
        sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>
    ) {
        database.persistSykmeldingsperioder(
            sykmeldingId = sykmeldingId,
            orgnumber = orgnumber,
            employeeIdentificationNumber = employeeIdentificationNumber,
            sykmeldingsperioder = sykmeldingsperioder
        )
    }

    fun deleteSykmeldingsperioder(sykmeldingId: String) {
        database.deleteSykmeldingsperioder(sykmeldingId)
    }

    fun getSykmeldingsperioder(
        orgnumber: String,
        employeeIdentificationNumber: String,
    ): List<Sykmeldingsperiode> {
        return database.getSykmeldingsperioder(orgnumber, employeeIdentificationNumber)
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
