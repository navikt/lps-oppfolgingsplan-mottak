package no.nav.syfo.domain

data class PersonIdent(val value: String) {
    private val elevenDigits = Regex("^\\d{11}\$")

    init {
        require(elevenDigits.matches(value))
    }
}
