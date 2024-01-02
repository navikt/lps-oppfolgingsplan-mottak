package no.nav.syfo.consumer.dokarkiv

data class Dokumentvariant(
    val filnavn: String,
    val filtype: String,
    val fysiskDokument: ByteArray,
    val variantformat: String,
)
