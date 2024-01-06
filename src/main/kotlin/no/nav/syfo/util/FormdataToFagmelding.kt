package no.nav.syfo.util

import no.nav.helse.op2016.Skjemainnhold
import no.nav.syfo.service.domain.*

fun mapFormdataToFagmelding(
    fnr: String,
    skjemainnhold: Skjemainnhold,
): Fagmelding = Fagmelding(
    oppfolgingsplan = Oppfolgingsplan(
        nokkelopplysninger = nokkelopplysninger(
            skjemainnhold,
        ),
        opplysningerOmArbeidstakeren = opplysningerOmArbeidstakeren(
            fnr,
            skjemainnhold,
        ),
        opplysingerOmSykefravaeret = opplysingerOmSykefravaeret(
            skjemainnhold
        ),
        tiltak =  tiltak(
            skjemainnhold
        ),
        arbeidstakersDeltakelse = arbeidstakersDeltakelse(
            skjemainnhold
        ),
        utfyllendeInfo = skjemainnhold.utfyllendeOpplysninger
    )
)

fun nokkelopplysninger(
    skjemainnhold: Skjemainnhold,
) = Nokkelopplysninger(
    virksomhetensnavn = skjemainnhold.arbeidsgiver.orgnavn,
    organiasjonsnr = skjemainnhold.arbeidsgiver.orgnr,
    nearmestelederFornavnEtternavn = FornavnEtternavn(
        fornavn = skjemainnhold.arbeidsgiver.naermesteLederFornavn,
        etternavn = skjemainnhold.arbeidsgiver.naermesteLederEtternavn
    ),
    tlfnearmesteleder = skjemainnhold.arbeidsgiver.telefonNaermesteLeder,
    annenKontaktPersonFornavnEtternavn = FornavnEtternavn(
        fornavn = skjemainnhold.arbeidsgiver.annenKontaktpersonFornavn,
        etternavn = skjemainnhold.arbeidsgiver.annenKontaktpersonEtternavn
    ),
    tlfkontatkperson = skjemainnhold.arbeidsgiver.telefonKontaktperson,
    virksomhetenerIAVirksomhet = skjemainnhold.arbeidsgiver.isVirksomhetErIABedrift,
    virksomhetenHarBedrifsHelseTjeneste = skjemainnhold.arbeidsgiver.isVirksomhetHarBedriftshelsetjeneste
)

fun opplysningerOmArbeidstakeren(
    fnr: String,
    skjemainnhold: Skjemainnhold,
) = OpplysningerOmArbeidstakeren(
    arbeidstakerenFornavnEtternavn = if (skjemainnhold.arbeidstakerHasNoName()) null
    else FornavnEtternavn(
        fornavn = skjemainnhold.sykmeldtArbeidstaker.fornavn,
        etternavn = skjemainnhold.sykmeldtArbeidstaker.etternavn
    ),
    fodselsnummer = fnr,
    tlf = skjemainnhold.sykmeldtArbeidstaker.tlf,
    stillingAvdeling = skjemainnhold.sykmeldtArbeidstaker.stillingAvdeling,
    ordineareArbeidsoppgaver = skjemainnhold.sykmeldtArbeidstaker.ordinaereArbeidsoppgaver
)

fun opplysingerOmSykefravaeret(
    skjemainnhold: Skjemainnhold
) = if (
    skjemainnhold.sykefravaerForSykmeldtArbeidstaker?.foersteFravaersdag == null
    && skjemainnhold.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsdato == null
    && skjemainnhold.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsprosentVedSykmeldingsDato == null
) null else OpplysingerOmSykefravaeret(
    forsteFravearsdag = skjemainnhold.sykefravaerForSykmeldtArbeidstaker?.foersteFravaersdag,
    sykmeldingsDato = skjemainnhold.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsdato,
    sykmeldingsProsentVedSykmeldDato = skjemainnhold.sykefravaerForSykmeldtArbeidstaker
        ?.sykmeldingsprosentVedSykmeldingsDato
)

fun tiltak(
    skjemainnhold: Skjemainnhold
) = skjemainnhold.tiltak.tiltaksinformasjon.map {
    Tiltak(
        ordineareArbeidsoppgaverSomKanIkkeKanUtfores = it.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres,
        beskrivelseAvTiltak = it.beskrivelseAvTiltaket,
        maalMedTiltaket = it.maalMedTiltaket,
        tiltaketGjennonforesIPerioden = TiltaketGjennonforesIPerioden(
            fraDato = it?.tidsrom?.periodeFra,
            tilDato = it?.tidsrom?.periodeTil
        ),
        tilrettelagtArbeidIkkeMulig = it.tilrettelagtArbeidIkkeMulig,
        sykmeldingsprosendIPerioden = it.sykmeldingsprosentIPerioden,
        behovForBistandFraNav = if (
            listOf<Any?>(
                it.isBistandRaadOgVeiledning,
                it.bistandRaadOgVeiledningBeskrivelse,
                it.isBistandDialogMoeteMedNav,
                it.bistandDialogMoeteMedNavBeskrivelse,
                it.isBistandArbeidsrettedeTiltakOgVirkemidler,
                it.bistandArbeidsrettedeTiltakOgVirkemidlerBeskrivelse,
                it.isBistandHjelpemidler,
                it.bistandHjelpemidlerBeskrivelse,
            ).any { behov -> behov == null }
        ) null else BehovForBistandFraNav(
            raadOgVeiledning = it.isBistandRaadOgVeiledning,
            raadOgVeiledningBeskrivelse = it.bistandRaadOgVeiledningBeskrivelse,
            dialogmoteMed = it.isBistandDialogMoeteMedNav,
            dialogmoteMedBeskrivelse = it.bistandDialogMoeteMedNavBeskrivelse,
            arbeidsrettedeTiltak = it.isBistandArbeidsrettedeTiltakOgVirkemidler,
            arbeidsrettedeTiltakBeskrivelse = it.bistandArbeidsrettedeTiltakOgVirkemidlerBeskrivelse,
            hjelpemidler = it.isBistandHjelpemidler,
            hjelpemidlerBeskrivelse = it.bistandHjelpemidlerBeskrivelse
        ),
        behovForBistandFraAndre = if (
            it.isBistandBedriftshelsetjenesten == null
            && it.isBistandAndre == null
            && it.isBistandBedriftshelsetjenesten == null
        ) null else BehovForBistandFraAndre(
            bedriftsHelsetjenesten = it.isBistandBedriftshelsetjenesten,
            andre = it.isBistandAndre,
            andreFritekst = it.bistandAndreBeskrivelse
        ),
        behovForAvklaringMedLegeSykmelder = it.behovForAvklaringLegeSykmelder,
        vurderingEffektAvTiltak = VurderingEffektAvTiltak(
            behovForNyeTiltak = it.isBehovForNyeTiltak,
            vurderingEffektAvTiltakFritekst = it.vurderingAvTiltak
        ),
        fremdrift = it.oppfoelgingssamtaler,
        underskrift = Underskift(
            datoforUnderskift = it.underskriftsdato,
            signertPapirkopiForeliggerPaaArbeidsplasssen = it.isSignertPapirkopiForeligger
        )
    )
}

fun arbeidstakersDeltakelse(
    skjemainnhold: Skjemainnhold
) = skjemainnhold.arbeidstakersDeltakelse?.let {
    ArbeidstakersDeltakelse(
        arbeidstakerMedvirkGjeonnforingOppfolginsplan = it.isArbeidstakerMedvirketGjennomfoering,
        hvorforHarIkkeArbeidstakerenMedvirket = it.arbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse
    )
}

private fun Skjemainnhold.arbeidstakerHasNoName() =
    this.sykmeldtArbeidstaker.fornavn.isNullOrBlank() && this.sykmeldtArbeidstaker.etternavn.isNullOrBlank()
