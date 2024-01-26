package no.nav.syfo.client.dokarkiv.domain

data class Dokumentvariant(
    val filnavn: String,
    val filtype: String,
    val fysiskDokument: ByteArray,
    val variantformat: String,
)
