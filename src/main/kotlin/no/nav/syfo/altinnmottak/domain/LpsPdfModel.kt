package no.nav.syfo.altinnmottak.domain

import java.time.LocalDate

data class Fagmelding(
    val oppfolgingsplan: Oppfolgingsplan
)

data class Oppfolgingsplan(
    val nokkelopplysninger: Nokkelopplysninger,
    val opplysningerOmArbeidstakeren: OpplysningerOmArbeidstakeren,
    val opplysingerOmSykefravaeret: OpplysingerOmSykefravaeret?,
    val tiltak: List<Tiltak>,
    val arbeidstakersDeltakelse: ArbeidstakersDeltakelse?,
    val utfyllendeInfo: String?
)

data class Nokkelopplysninger(
    val virksomhetensnavn: String?,
    val organiasjonsnr: String?,
    val nearmestelederFornavnEtternavn: FornavnEtternavn?,
    val tlfnearmesteleder: String?,
    val annenKontaktPersonFornavnEtternavn: FornavnEtternavn?,
    val tlfkontatkperson: String?,
    val virksomhetenerIAVirksomhet: Boolean?,
    val virksomhetenHarBedrifsHelseTjeneste: Boolean?
)

data class OpplysningerOmArbeidstakeren(
    val arbeidstakerenFornavnEtternavn: FornavnEtternavn?,
    val fodselsnummer: String?,
    val tlf: String?,
    val stillingAvdeling: String?,
    val ordineareArbeidsoppgaver: String?
)

data class OpplysingerOmSykefravaeret(
    val forsteFravearsdag: LocalDate?,
    val sykmeldingsDato: LocalDate?,
    val sykmeldingsProsentVedSykmeldDato: String?
)

data class Tiltak(
    val ordineareArbeidsoppgaverSomKanIkkeKanUtfores: String?,
    val beskrivelseAvTiltak: String?,
    val maalMedTiltaket: String?,
    val tiltaketGjennonforesIPerioden: TiltaketGjennonforesIPerioden,
    val sykmeldingsprosendIPerioden: String?,
    val behovForBistandFraNav: BehovForBistandFraNav?,
    val behovForBistandFraAndre: BehovForBistandFraAndre?,
    val behovForAvklaringMedLegeSykmelder: String?,
    val tilrettelagtArbeidIkkeMulig: String?,
    val vurderingEffektAvTiltak: VurderingEffektAvTiltak,
    val fremdrift: String?,
    val underskrift: Underskift
)

data class TiltaketGjennonforesIPerioden(
    val fraDato: LocalDate?,
    val tilDato: LocalDate?
)

data class BehovForBistandFraNav(
    val raadOgVeiledning: Boolean?,
    val raadOgVeiledningBeskrivelse: String?,
    val dialogmoteMed: Boolean?,
    val dialogmoteMedBeskrivelse: String?,
    val arbeidsrettedeTiltak: Boolean?,
    val arbeidsrettedeTiltakBeskrivelse: String?,
    val hjelpemidler: Boolean?,
    val hjelpemidlerBeskrivelse: String?
)

data class BehovForBistandFraAndre(
    val bedriftsHelsetjenesten: Boolean?,
    val andre: Boolean?,
    val andreFritekst: String?
)

data class VurderingEffektAvTiltak(
    val vurderingEffektAvTiltakFritekst: String?,
    val behovForNyeTiltak: Boolean?
)

data class Underskift(
    val datoforUnderskift: LocalDate?,
    val signertPapirkopiForeliggerPaaArbeidsplasssen: Boolean?
)

data class FornavnEtternavn(
    val fornavn: String?,
    val etternavn: String?
)

data class ArbeidstakersDeltakelse(
    val arbeidstakerMedvirkGjeonnforingOppfolginsplan: Boolean?,
    val hvorforHarIkkeArbeidstakerenMedvirket: String?
)

fun Oppfolgingsplan.isBehovForBistandFraNAV(): Boolean {
    return this.tiltak.any {
        it.behovForBistandFraNav?.raadOgVeiledning == true
            || it.behovForBistandFraNav?.dialogmoteMed == true
            || it.behovForBistandFraNav?.arbeidsrettedeTiltak == true
            || it.behovForBistandFraNav?.hjelpemidler == true
    }
}
