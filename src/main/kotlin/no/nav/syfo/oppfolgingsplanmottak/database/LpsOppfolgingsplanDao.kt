package no.nav.syfo.oppfolgingsplanmottak.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toObject
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanResponse
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@Suppress("MagicNumber", "LongMethod")
fun DatabaseInterface.storeFollowUpPlan(
    uuid: UUID,
    followUpPlanDTO: FollowUpPlanDTO,
    organizationNumber: String,
    lpsOrgnumber: String,
    sentToGeneralPractitionerAt: LocalDateTime?,
) {
    val sentToGeneralPractitionerAtValueToStore =
        if (sentToGeneralPractitionerAt != null)
            {
                Timestamp.valueOf(sentToGeneralPractitionerAt)
            } else {
            sentToGeneralPractitionerAt
        }

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
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            it.setBoolean(18, followUpPlanDTO.employeeHasContributedToPlan)
            it.setString(19, followUpPlanDTO.employeeHasNotContributedToPlanDescription)
            it.setBytes(20, null)
            it.setBoolean(21, followUpPlanDTO.sendPlanToNav)
            it.setTimestamp(22, null)
            it.setBoolean(23, followUpPlanDTO.sendPlanToGeneralPractitioner)
            it.setTimestamp(24, sentToGeneralPractitionerAtValueToStore)
            it.setInt(25, 0)
            it.setString(26, null)
            it.setString(27, followUpPlanDTO.lpsName)
            it.setString(28, lpsOrgnumber)
            it.setTimestamp(29, Timestamp.valueOf(LocalDateTime.now()))
            it.setTimestamp(30, Timestamp.valueOf(LocalDateTime.now()))
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

fun DatabaseInterface.findSendingStatus(uuid: UUID): FollowUpPlanResponse {
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

fun ResultSet.toFollowUpPlanSendingStatus() =
    FollowUpPlanResponse(
        uuid = getString("uuid"),
        isSentToGeneralPractitionerStatus = getTimestamp("sent_to_general_practitioner_at") != null,
        isSentToNavStatus = getTimestamp("sent_to_nav_at") != null,
    )
