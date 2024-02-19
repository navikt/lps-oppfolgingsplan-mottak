package no.nav.syfo.mockdata

import no.nav.syfo.domain.PersonIdent

object UserConstants {
    const val VEILEDER_IDENT = "Z999999"
    private const val ARBEIDSTAKER_FNR = "12345678912"

    val ARBEIDSTAKER_PERSONIDENT = PersonIdent(ARBEIDSTAKER_FNR)
    val PERSONIDENT_VEILEDER_NO_ACCESS = PersonIdent(ARBEIDSTAKER_PERSONIDENT.value.replace("3", "1"))
}
