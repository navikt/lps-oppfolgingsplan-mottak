package no.nav.syfo.api.lps

import java.time.LocalDateTime

data class OppfolgingsplanDTO(
    val oppfolgingsplanMeta: OppfolgingsplanMeta,
    val arbeidssituasjon: Arbeidssituasjon,
    val tilrettelegging: Tilrettelegging,
    val behovForBistandFraNAV: String?,
    val behovForAvklaringMedSykmelder: String?,
    val utfyllendeOpplysninger: String?
)

data class OppfolgingsplanMeta(
    val utfyllingsdato: LocalDateTime,
    val mottaker: Mottaker,
    val sykmeldtFnr: String,
    val virksomhet: Virksomhet,
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

data class Virksomhet(
    val virksomhetsnavn: String,
    val virksomhetsnummer: String,
    val naermesteLederFornavn: String,
    val naermesteLederEtternavn: String,
    val telefonNaermesteLeder: String,
)

enum class Mottaker {
    NAV, FASTLEGE, NAVOGFASTLEGE
}
