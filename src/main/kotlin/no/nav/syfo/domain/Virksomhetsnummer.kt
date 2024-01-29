package no.nav.syfo.domain

data class Virksomhetsnummer(
    val value: String,
) {
    private val nineDigits = Regex("^\\d{9}\$")

    init {
        require(nineDigits.matches(value))
    }
}
