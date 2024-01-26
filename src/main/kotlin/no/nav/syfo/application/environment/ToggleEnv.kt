package no.nav.syfo.application.environment

data class ToggleEnv(
    val sendToFastlegeToggle: Boolean,
    val sendToNavToggle: Boolean,
    val journalforToggle: Boolean,
)
