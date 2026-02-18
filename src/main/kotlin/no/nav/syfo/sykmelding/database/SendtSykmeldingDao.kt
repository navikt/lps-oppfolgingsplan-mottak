package no.nav.syfo.sykmelding.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun DatabaseInterface.persistSykmeldingsperiode(
    sykmeldingId: String,
    orgnummer: String,
    employeeIdentificationNumber: String,
    fom: LocalDate,
    tom: LocalDate,
) {
    val insertStatement =
        """
        INSERT INTO SYKMELDINGSPERIODE (
            uuid,
            sykmelding_id,
            organization_number,
            employee_identification_number,
            fom,
            tom,
            created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setObject(1, UUID.randomUUID())
            it.setString(2, sykmeldingId)
            it.setString(3, orgnummer)
            it.setString(4, employeeIdentificationNumber)
            it.setTimestamp(5, Timestamp.valueOf(fom.atStartOfDay()))
            it.setTimestamp(6, Timestamp.valueOf(tom.atStartOfDay()))
            it.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()))
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.deleteSykmeldingsperioder(sykmeldingId: String) {
    val deleteStatement =
        """
        DELETE FROM SYKMELDINGSPERIODE
        WHERE sykmelding_id = ?
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(deleteStatement).use {
            it.setString(1, sykmeldingId)
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.getSykmeldingsperioder(
    orgnumber: String,
    employeeIdentificationNumber: String,
): List<Sykmeldingsperiode> {
    val selectStatement =
        """
        SELECT *
        FROM SYKMELDINGSPERIODE
        WHERE organization_number = ? AND employee_identification_number = ?
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(selectStatement).use {
            it.setString(1, orgnumber)
            it.setString(2, employeeIdentificationNumber)
            it.executeQuery().toList { toSykmeldingsperiode() }
        }
    }
}

fun DatabaseInterface.getActiveSendtSykmeldingsperioder(employeeIdentificationNumber: String): List<Sykmeldingsperiode> {
    val today = Timestamp.valueOf(LocalDateTime.now())
    val selectStatement =
        """
        SELECT *
        FROM SYKMELDINGSPERIODE
        WHERE employee_identification_number = ? AND ? BETWEEN fom AND (tom + INTERVAL '16 days')
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(selectStatement).use {
            it.setString(1, employeeIdentificationNumber)
            it.setObject(2, today)
            it.executeQuery().toList { toSykmeldingsperiode() }
        }
    }
}

fun ResultSet.toSykmeldingsperiode() =
    Sykmeldingsperiode(
        uuid = UUID.fromString(getString("uuid")),
        sykmeldingId = getString("sykmelding_id"),
        organizationNumber = getString("organization_number"),
        employeeIdentificationNumber = getString("employee_identification_number"),
        fom = getTimestamp("fom").toLocalDateTime().toLocalDate(),
        tom = getTimestamp("tom").toLocalDateTime().toLocalDate(),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
    )
