package no.nav.syfo.client.dokarkiv.domain

data class Dokument(
    val brevkode: String,
    val dokumentKategori: String,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String,
)
