package no.nav.syfo.oppfolgingsplanmottak.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toObject
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLNonTransientException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@Suppress("MagicNumber", "LongMethod")
fun DatabaseInterface.storeFollowUpPlan(
    uuid: UUID,
    followUpPlanDTO: FollowUpPlanDTO,
    organizationNumber: String,
    lpsOrgnumber: String,
    sentToGeneralPractitionerAt: Timestamp?,
    sentToNavAt: Timestamp?,
) {
    val insertStatement =
        """
            INSERT INTO FOLLOW_UP_PLAN_LPS_V1 (
            uuid,
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
            contact_person_email,
            employee_has_contributed_to_plan,
            employee_has_not_contributed_to_plan_description,
            pdf,
            send_plan_to_nav,
            sent_to_nav_at,
            send_plan_to_general_practitioner,
            sent_to_general_practitioner_at,
            send_to_general_practitioner_retry_count,
            journalpost_id,
            lps_name,
            lps_orgnumber,
            created_at,
            last_updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setObject(1, uuid)
            it.setString(2, organizationNumber)
            it.setString(3, followUpPlanDTO.employeeIdentificationNumber)
            it.setString(4, followUpPlanDTO.typicalWorkday)
            it.setString(5, followUpPlanDTO.tasksThatCanStillBeDone)
            it.setString(6, followUpPlanDTO.tasksThatCanNotBeDone)
            it.setString(7, followUpPlanDTO.previousFacilitation)
            it.setString(8, followUpPlanDTO.plannedFacilitation)
            it.setString(9, followUpPlanDTO.otherFacilitationOptions)
            it.setString(10, followUpPlanDTO.followUp)
            it.setDate(11, Date.valueOf(followUpPlanDTO.evaluationDate))
            it.setObject(12, followUpPlanDTO.needsHelpFromNav)
            it.setString(13, followUpPlanDTO.needsHelpFromNavDescription)
            it.setString(14, followUpPlanDTO.messageToGeneralPractitioner)
            it.setString(15, followUpPlanDTO.additionalInformation)
            it.setString(16, followUpPlanDTO.contactPersonFullName)
            it.setString(17, followUpPlanDTO.contactPersonPhoneNumber)
            it.setString(18, followUpPlanDTO.contactPersonEmail)
            it.setBoolean(19, followUpPlanDTO.employeeHasContributedToPlan)
            it.setString(20, followUpPlanDTO.employeeHasNotContributedToPlanDescription)
            it.setBytes(21, null)
            it.setBoolean(22, followUpPlanDTO.sendPlanToNav)
            it.setTimestamp(23, sentToNavAt)
            it.setBoolean(24, followUpPlanDTO.sendPlanToGeneralPractitioner)
            it.setTimestamp(25, sentToGeneralPractitionerAt)
            it.setInt(26, 0)
            it.setString(27, null)
            it.setString(28, followUpPlanDTO.lpsName)
            it.setString(29, lpsOrgnumber)
            it.setTimestamp(30, Timestamp.valueOf(LocalDateTime.now()))
            it.setTimestamp(31, Timestamp.valueOf(LocalDateTime.now()))
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.storeLpsPdf(
    uuid: UUID,
    pdfBytes: ByteArray,
): Int {
    val updateStatement =
        """
        UPDATE FOLLOW_UP_PLAN_LPS_V1
        SET pdf = ?, last_updated_at = ?
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated =
            connection.prepareStatement(updateStatement).use {
                it.setBytes(1, pdfBytes)
                it.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                it.setObject(3, uuid)
                it.executeUpdate()
            }
        connection.commit()
        rowsUpdated
    }
}

@Suppress("SwallowedException")
fun DatabaseInterface.findSendingStatus(uuid: UUID): FollowUpPlanResponse? {
    return try {
        this.findFollowUpPlanResponseById(uuid)
    } catch (e: SQLNonTransientException) {
        null
    }
}

fun DatabaseInterface.findFollowUpPlanResponseById(uuid: UUID): FollowUpPlanResponse? {
    val queryStatement =
        """
        SELECT *
        FROM FOLLOW_UP_PLAN_LPS_V1
        WHERE uuid = ?
        """.trimIndent()

    return connection.use { connection ->
        connection.prepareStatement(queryStatement).use {
            it.setObject(1, uuid)
            it.executeQuery().toObject { toFollowUpPlanSendingStatus() }
        }
    }
}

fun DatabaseInterface.updateSentAt(
    uuid: UUID,
    sentToGeneralPractitionerAt: Timestamp?,
    sentToNavAt: Timestamp?,
    pdf: ByteArray?,
): Int {
    val updateStatement = """
        UPDATE FOLLOW_UP_PLAN_LPS_V1
        SET sent_to_general_practitioner_at = ?, sent_to_nav_at = ?, pdf = ?
        WHERE uuid = ?
    """.trimIndent()

    return connection.use { connection ->
        val rowsUpdated = connection.prepareStatement(updateStatement).use {
            it.setTimestamp(1, sentToGeneralPractitionerAt)
            it.setTimestamp(2, sentToNavAt)
            it.setBytes(3, pdf)
            it.setObject(4, uuid)
            it.executeUpdate()
        }
        connection.commit()
        rowsUpdated
    }
}

fun ResultSet.toFollowUpPlanSendingStatus() =
    FollowUpPlanResponse(
        uuid = getString("uuid"),
        isSentToGeneralPractitionerStatus = getTimestamp("sent_to_general_practitioner_at") != null,
        isSentToNavStatus = getTimestamp("sent_to_nav_at") != null,
    )
