package no.nav.syfo.api.lps.dto

data class OppfolgingsplanPdfDTO(
    val oppfolgingsplanMeta: OppfolgingsplanMeta,
    val oppfolgingsplanPdf: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OppfolgingsplanPdfDTO

        if (oppfolgingsplanMeta != other.oppfolgingsplanMeta) return false
        return oppfolgingsplanPdf.contentEquals(other.oppfolgingsplanPdf)
    }

    override fun hashCode(): Int {
        var result = oppfolgingsplanMeta.hashCode()
        result = 31 * result + oppfolgingsplanPdf.contentHashCode()
        return result
    }
}
