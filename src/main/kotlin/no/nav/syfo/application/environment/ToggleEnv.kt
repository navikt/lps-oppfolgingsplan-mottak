package no.nav.syfo.application.environment

data class ToggleEnv(
    val sendAltinnLpsPlanToFastlegeToggle: Boolean,
    val sendAltinnLpsPlanToNavToggle: Boolean,
    val journalforAltinnLpsPlanToggle: Boolean,
)
