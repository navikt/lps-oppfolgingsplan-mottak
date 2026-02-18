package no.nav.syfo.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.op2016.Skjemainnhold
import no.nav.helse.op2016.Tiltak

class FormdataToFagmeldingKtTest :
    DescribeSpec({
        describe("Mapping of Tiltak") {
            context("behovForBistandFraNav") {
                it("behovForBistandFraNav should be null when all values are null") {
                    val skjemainnhold = mockk<Skjemainnhold>(relaxed = true)
                    val tiltak = mockk<Tiltak>(relaxed = true)
                    val tiltaksinformasjon = mockk<Tiltak.Tiltaksinformasjon>(relaxed = true)

                    every { skjemainnhold.tiltak } returns tiltak
                    every { tiltak.tiltaksinformasjon } returns mutableListOf(tiltaksinformasjon)
                    every { tiltaksinformasjon.isBistandRaadOgVeiledning } returns null
                    every { tiltaksinformasjon.bistandRaadOgVeiledningBeskrivelse } returns null
                    every { tiltaksinformasjon.isBistandDialogMoeteMedNav } returns null
                    every { tiltaksinformasjon.bistandDialogMoeteMedNavBeskrivelse } returns null
                    every { tiltaksinformasjon.isBistandArbeidsrettedeTiltakOgVirkemidler } returns null
                    every { tiltaksinformasjon.bistandArbeidsrettedeTiltakOgVirkemidlerBeskrivelse } returns null
                    every { tiltaksinformasjon.isBistandHjelpemidler } returns null
                    every { tiltaksinformasjon.bistandHjelpemidlerBeskrivelse } returns null

                    val listOfMappedTiltak = tiltak(skjemainnhold)
                    listOfMappedTiltak.first().behovForBistandFraNav shouldBe null
                }

                it("behovForBistandFraNav should not be null if any of the values are not null") {
                    val skjemainnhold = mockk<Skjemainnhold>(relaxed = true)
                    val tiltak = mockk<Tiltak>(relaxed = true)
                    val tiltaksinformasjon = mockk<Tiltak.Tiltaksinformasjon>(relaxed = true)

                    every { skjemainnhold.tiltak } returns tiltak
                    every { tiltak.tiltaksinformasjon } returns mutableListOf(tiltaksinformasjon)
                    every { tiltaksinformasjon.isBistandRaadOgVeiledning } returns null
                    every { tiltaksinformasjon.bistandRaadOgVeiledningBeskrivelse } returns null
                    every { tiltaksinformasjon.isBistandDialogMoeteMedNav } returns true
                    every { tiltaksinformasjon.bistandDialogMoeteMedNavBeskrivelse } returns null
                    every { tiltaksinformasjon.isBistandArbeidsrettedeTiltakOgVirkemidler } returns null
                    every { tiltaksinformasjon.bistandArbeidsrettedeTiltakOgVirkemidlerBeskrivelse } returns null
                    every { tiltaksinformasjon.isBistandHjelpemidler } returns null
                    every { tiltaksinformasjon.bistandHjelpemidlerBeskrivelse } returns null

                    val listOfMappedTiltak = tiltak(skjemainnhold)
                    listOfMappedTiltak.first().behovForBistandFraNav shouldNotBe null
                }
            }
        }
    })
