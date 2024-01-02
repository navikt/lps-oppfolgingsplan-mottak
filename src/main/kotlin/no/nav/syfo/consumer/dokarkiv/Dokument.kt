package no.nav.syfo.consumer.dokarkiv

data class Dokument(
    val brevkode: String,
    val dokumentKategori: String,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String,
)
