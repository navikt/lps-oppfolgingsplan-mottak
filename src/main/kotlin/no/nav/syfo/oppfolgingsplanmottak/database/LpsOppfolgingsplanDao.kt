package no.nav.syfo.oppfolgingsplanmottak.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanDTO
import no.nav.syfo.util.gsonSerializer
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

val gsonSerializer = gsonSerializer()

@Suppress("MagicNumber")
fun DatabaseInterface.storeLps(
    uuid: UUID,
    oppfolgingsplanDTO: FollowUpPlanDTO,
    virksomhetsnummer: String,
    pdf: ByteArray
) {
    val insertStatement = """
    INSERT INTO FOLLOW_UP_PLAN_LPS_V1
    (uuid,
     organization_number,
     employee_identification_number,
     typical_workday,
     tasks_that_can_still_be_done,
     tasks_that_can_not_be_done,
     previous_facilitation,
     planned_facilitation,
     other_facilitation_options,
     follow_up,
     evaluation_date,
     needs_help_from_nav,
     needs_help_from_nav_description,
     message_to_general_practitioner,
     additional_information,
     contact_person_full_name,
     contact_person_phone_number,
     employee_has_contributed_to_plan,
     employee_has_not_contributed_to_plan_description,
     pdf,
     send_plan_to_nav,
     send_plan_to_general_practitioner,
     created_at,
     last_updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    val now = Timestamp.valueOf(LocalDateTime.now())

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setObject(1, uuid)
            it.setString(2, virksomhetsnummer)
            it.setString(3, oppfolgingsplanDTO.employeeIdentificationNumber)
            it.setString(4, oppfolgingsplanDTO.typicalWorkday)
            it.setString(5, oppfolgingsplanDTO.tasksThatCanStillBeDone)
            it.setString(6, oppfolgingsplanDTO.tasksThatCanNotBeDone)
            it.setString(7, oppfolgingsplanDTO.previousFacilitation)
            it.setString(8, oppfolgingsplanDTO.plannedFacilitation)
            it.setString(9, oppfolgingsplanDTO.otherFacilitationOptions)
            it.setString(10, oppfolgingsplanDTO.followUp)
            it.setDate(11, Date.valueOf(oppfolgingsplanDTO.evaluationDate))
            it.setObject(12, oppfolgingsplanDTO.needsHelpFromNav)
            it.setString(13, oppfolgingsplanDTO.needsHelpFromNavDescription)
            it.setString(14, oppfolgingsplanDTO.messageToGeneralPractitioner)
            it.setString(15, oppfolgingsplanDTO.additionalInformation)
            it.setString(16, oppfolgingsplanDTO.contactPersonFullName)
            it.setString(17, oppfolgingsplanDTO.contactPersonPhoneNumber)
            it.setBoolean(18, oppfolgingsplanDTO.employeeHasContributedToPlan)
            it.setString(19, oppfolgingsplanDTO.employeeHasNotContributedToPlanDescription)
            it.setBytes(20, pdf)
            it.setBoolean(21, oppfolgingsplanDTO.sendPlanToNav)
            it.setBoolean(22, oppfolgingsplanDTO.sendPlanToGeneralPractitioner)
            it.setTimestamp(23, now)
            it.setTimestamp(24, now)
            it.executeUpdate()
        }
        connection.commit()
    }
}

@Suppress("MagicNumber")
fun DatabaseInterface.storeLps(oppfolgingsplanDTO: OppfolgingsplanDTO, version: Short) {
    val insertStatement = """
        INSERT INTO OPPFOLGINGSPLAN_LPS (
            uuid,
            fnr,
            virksomhetsnummer,
            mottaker,
            utfyllingsdato,
            innhold,
            versjon,
            opprettet,
            sist_endret
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()

    val now = Timestamp.valueOf(LocalDateTime.now())
    val entryUuid = UUID.randomUUID()
    val metadata = oppfolgingsplanDTO.oppfolgingsplanMeta
    val fnr = metadata.sykmeldtFnr
    val virksomhetsnummer = metadata.virksomhet.virksomhetsnummer
    val mottaker = "${metadata.mottaker}"
    val utfyllingsdato = metadata.utfyllingsdato
    val content = gsonSerializer.toJson(oppfolgingsplanDTO).trim()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setObject(1, entryUuid)
            it.setString(2, fnr)
            it.setString(3, virksomhetsnummer)
            it.setString(4, mottaker)
            it.setTimestamp(5, Timestamp.valueOf(utfyllingsdato))
            it.setString(6, content)
            it.setShort(7, version)
            it.setTimestamp(8, now)
            it.setTimestamp(9, now)
            it.executeUpdate()
        }
        connection.commit()
    }
}
