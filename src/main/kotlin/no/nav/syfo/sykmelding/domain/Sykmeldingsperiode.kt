package no.nav.syfo.sykmelding.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Sykmeldingsperiode(
    val uuid: UUID,
    val sykmeldingId: String,
    val organizationNumber: String,
    val employeeIdentificationNumber: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val createdAt: LocalDateTime
)
