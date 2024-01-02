package no.nav.syfo.consumer.dokarkiv

data class JournalpostResponse(
    val dokumenter: List<DokumentInfo>,
    val journalpostId: Int,
    val journalpostferdigstilt: Boolean,
    val journalstatus: String,
    val melding: String,
)
