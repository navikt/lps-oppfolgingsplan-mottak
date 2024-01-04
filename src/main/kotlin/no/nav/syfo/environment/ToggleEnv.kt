package no.nav.syfo.environment

data class ToggleEnv(
    val sendToFastlegeToggle: Boolean,
    val sendToNavToggle: Boolean,
    val journalforToggle: Boolean,
)
