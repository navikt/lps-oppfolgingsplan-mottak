package no.nav.syfo.sykmelding.domain

import java.time.LocalDate

data class SykmeldingKafkaMessage(
    val sykmelding: ArbeidsgiverSykmelding,
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SykmeldingStatusKafkaEventDTO,
)

data class KafkaMetadataDTO(
    val sykmeldingId: String,
    val fnr: String,
)

data class ArbeidsgiverStatusKafkaDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String? = null,
    val orgNavn: String,
)

data class SykmeldingStatusKafkaEventDTO(
    val sykmeldingId: String,
    val arbeidsgiver: ArbeidsgiverStatusKafkaDTO,
)

data class ArbeidsgiverSykmelding(
    val sykmeldingsperioder: List<SykmeldingsperiodeAGDTO>,
)

data class SykmeldingsperiodeAGDTO(
    val fom: LocalDate,
    val tom: LocalDate,
)
