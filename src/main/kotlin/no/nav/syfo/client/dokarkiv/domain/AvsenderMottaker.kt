package no.nav.syfo.client.dokarkiv.domain

data class AvsenderMottaker(
    val id: String,
    val idType: String,
    val navn: String?, // when idType is ORGNR and navn is null joark will look up the name
)
