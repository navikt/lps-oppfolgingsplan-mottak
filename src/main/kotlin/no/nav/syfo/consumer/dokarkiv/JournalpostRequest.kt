package no.nav.syfo.consumer.dokarkiv

data class JournalpostRequest(
    val avsenderMottaker: AvsenderMottaker,
    val tittel: String,
    val bruker: Bruker,
    val dokumenter: List<Dokument>,
    val journalfoerendeEnhet: Int,
    val journalpostType: String,
    val kanal: String,
    val sak: Sak,
    val tema: String,
)
