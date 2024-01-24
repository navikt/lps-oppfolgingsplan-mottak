package no.nav.syfo.environment

data class ToggleEnv(
    val sendAltinnLpsPlanToFastlegeToggle: Boolean,
    val sendAltinnLpsPlanToNavToggle: Boolean,
    val journalforAltinnLpsPlanToggle: Boolean,
    val sendLpsPlanToFastlegeToggle: Boolean,
    val sendLpsPlanToNavToggle: Boolean,
    val journalforLpsPlanToggle: Boolean,
)
