package no.nav.syfo.api.lps.dto

data class OppfolgingsplanDTO(
    val oppfolgingsplanMeta: OppfolgingsplanMeta,
    val arbeidssituasjon: Arbeidssituasjon,
    val tilrettelegging: Tilrettelegging,
    val behovForBistandFraNAV: String?,
    val behovForAvklaringMedSykmelder: String?,
    val utfyllendeOpplysninger: String?
)

data class Arbeidssituasjon(
    val stillingAvdeling: String,
    val ordinaereArbeidsoppgaver: String,
    val ordinaereArbeidsoppgaverSomIkkeKanUtfoeres: String?,
)

data class Tilrettelegging(
    val hvaHarBlittForsokt: String?,
    val tilretteleggingIDag: String,
    val fremtidigePlaner: String?,
)


