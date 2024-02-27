package no.nav.syfo.oppfolgingsplanmottak.domain

import java.io.Serializable

data class FollowUpPlanResponse(
    val uuid: String,
    val sentToGeneralPractitionerStatus: Boolean?,
    val sentToNavStatus: Boolean?,
) : Serializable
