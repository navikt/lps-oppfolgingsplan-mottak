package no.nav.syfo.mockdata

import no.nav.syfo.oppfolgingsplanmottak.domain.Arbeidssituasjon
import no.nav.syfo.oppfolgingsplanmottak.domain.Mottaker
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplanmottak.domain.OppfolgingsplanMeta
import no.nav.syfo.oppfolgingsplanmottak.domain.Tilrettelegging
import no.nav.syfo.oppfolgingsplanmottak.domain.Virksomhet
import java.time.LocalDateTime

fun createOppfolgingsplanMeta(): OppfolgingsplanMeta {
    return OppfolgingsplanMeta(
        utfyllingsdato = LocalDateTime.now(),
        mottaker = Mottaker.NAVOGFASTLEGE,
        sykmeldtFnr = "123456789",
        virksomhet = Virksomhet(
            virksomhetsnavn = "Ørsta Rådhus",
            virksomhetsnummer = "123",
            naermesteLederFornavn = "Helge",
            naermesteLederEtternavn = "Hatt",
            telefonNaermesteLeder = "90000000",
        ),
    )
}

fun createArbeidssituasjon(): Arbeidssituasjon {
    return Arbeidssituasjon(
        stillingAvdeling = "Utvikler",
        ordinaereArbeidsoppgaver = "Utvikler en del greier",
        ordinaereArbeidsoppgaverSomIkkeKanUtfoeres = "Kan ikke bruke tastatur lenger",
    )
}

fun createTilrettelegging(): Tilrettelegging {
    return Tilrettelegging(
        hvaHarBlittForsokt = null,
        tilretteleggingIDag = "Byttet tastatur med VR-briller",
        fremtidigePlaner = null,
    )
}

fun createDefaultOppfolgingsplanDTOMock(): OppfolgingsplanDTO {
    return OppfolgingsplanDTO(
        oppfolgingsplanMeta = createOppfolgingsplanMeta(),
        arbeidssituasjon = createArbeidssituasjon(),
        tilrettelegging = createTilrettelegging(),
        behovForAvklaringMedSykmelder = null,
        behovForBistandFraNAV = null,
        utfyllendeOpplysninger = null,
    )
}
