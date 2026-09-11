package no.nav.syfo.client.ereg.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EregOrganisasjonHierarkiTest :
    DescribeSpec({
        describe("aggregerOrgnummereFraHierarki") {
            it("includes underenhet, organisasjonsledd and juridisk enhet from the hierarchy") {
                val hierarki =
                    EregOrganisasjon(
                        organisasjonsnummer = "895225282",
                        bestaarAvOrganisasjonsledd =
                            listOf(
                                EregOrganisasjonsleddWrapper(
                                    organisasjonsledd =
                                        EregOrganisasjonsledd(
                                            organisasjonsnummer = "976679512",
                                            inngaarIJuridiskEnheter = listOf(EregEnhetsRelasjon("938801363")),
                                        ),
                                ),
                            ),
                    )

                hierarki.aggregerOrgnummereFraHierarki() shouldBe setOf("895225282", "976679512", "938801363")
            }

            it("walks nested organisasjonsledd via organisasjonsleddOver") {
                val hierarki =
                    EregOrganisasjon(
                        organisasjonsnummer = "111111111",
                        bestaarAvOrganisasjonsledd =
                            listOf(
                                EregOrganisasjonsleddWrapper(
                                    organisasjonsledd =
                                        EregOrganisasjonsledd(
                                            organisasjonsnummer = "222222222",
                                            organisasjonsleddOver =
                                                listOf(
                                                    EregOrganisasjonsleddWrapper(
                                                        organisasjonsledd =
                                                            EregOrganisasjonsledd(
                                                                organisasjonsnummer = "333333333",
                                                                inngaarIJuridiskEnheter =
                                                                    listOf(EregEnhetsRelasjon("444444444")),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                    )

                hierarki.aggregerOrgnummereFraHierarki() shouldBe
                    setOf("111111111", "222222222", "333333333", "444444444")
            }

            it("includes juridisk enhet directly from top-level inngaarIJuridiskEnheter (two-level underenhet)") {
                val hierarki =
                    EregOrganisasjon(
                        organisasjonsnummer = "123456789",
                        inngaarIJuridiskEnheter = listOf(EregEnhetsRelasjon("987654321")),
                    )

                hierarki.aggregerOrgnummereFraHierarki() shouldBe setOf("123456789", "987654321")
            }

            it("does not double-process a shared ancestor reached via two paths (visited guard)") {
                val deltLedd =
                    EregOrganisasjonsledd(
                        organisasjonsnummer = "333333333",
                        inngaarIJuridiskEnheter = listOf(EregEnhetsRelasjon("444444444")),
                    )
                val hierarki =
                    EregOrganisasjon(
                        organisasjonsnummer = "111111111",
                        bestaarAvOrganisasjonsledd =
                            listOf(
                                EregOrganisasjonsleddWrapper(
                                    organisasjonsledd =
                                        EregOrganisasjonsledd(
                                            organisasjonsnummer = "222222222",
                                            organisasjonsleddOver =
                                                listOf(
                                                    EregOrganisasjonsleddWrapper(deltLedd),
                                                    EregOrganisasjonsleddWrapper(deltLedd),
                                                ),
                                        ),
                                ),
                            ),
                    )

                hierarki.aggregerOrgnummereFraHierarki() shouldBe
                    setOf("111111111", "222222222", "333333333", "444444444")
            }

            it("excludes a juridisk enhet whose gyldighetsperiode has expired") {
                val hierarki =
                    EregOrganisasjon(
                        organisasjonsnummer = "895225282",
                        inngaarIJuridiskEnheter =
                            listOf(
                                EregEnhetsRelasjon(
                                    organisasjonsnummer = "938801363",
                                    gyldighetsperiode = EregGyldighetsperiode(fom = "2000-01-01", tom = "2010-12-31"),
                                ),
                            ),
                    )

                hierarki.aggregerOrgnummereFraHierarki() shouldBe setOf("895225282")
            }

            it("excludes an organisasjonsledd whose gyldighetsperiode has expired") {
                val hierarki =
                    EregOrganisasjon(
                        organisasjonsnummer = "895225282",
                        bestaarAvOrganisasjonsledd =
                            listOf(
                                EregOrganisasjonsleddWrapper(
                                    organisasjonsledd =
                                        EregOrganisasjonsledd(
                                            organisasjonsnummer = "976679512",
                                            inngaarIJuridiskEnheter = listOf(EregEnhetsRelasjon("938801363")),
                                        ),
                                    gyldighetsperiode = EregGyldighetsperiode(fom = "2000-01-01", tom = "2010-12-31"),
                                ),
                            ),
                    )

                hierarki.aggregerOrgnummereFraHierarki() shouldBe setOf("895225282")
            }
        }
    })
