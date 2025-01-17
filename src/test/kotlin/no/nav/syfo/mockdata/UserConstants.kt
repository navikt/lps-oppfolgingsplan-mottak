package no.nav.syfo.mockdata

import no.nav.syfo.domain.PersonIdent

object UserConstants {
    const val VEILEDER_IDENT = "Z999999"
    const val ARBEIDSTAKER_FNR = "12345678912"
    const val ARBEIDSTAKER_FNR_NO_ARBEIDSFORHOLD = "12345678913"
    const val VIRKSOMHETSNUMMER = "123456789"
    const val HOVEDENHETSNUMMER = "124456789"
    const val LPS_VIRKSOMHETSNUMMER = "123456784"

    val ARBEIDSTAKER_PERSONIDENT = PersonIdent(ARBEIDSTAKER_FNR)
    val PERSONIDENT_VEILEDER_NO_ACCESS = PersonIdent(ARBEIDSTAKER_PERSONIDENT.value.replace("3", "1"))
}
