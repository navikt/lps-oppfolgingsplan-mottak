package no.nav.syfo.client.pdl.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PdlResponseTest : DescribeSpec({
    describe("Validation of employer information") {
        it("Should set correct space between mame with middle name") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("Ola", "Veldig", "Normann")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", "1234"))),
                    )
                )
            )

            val name = pdlResponse.data!!.toPersonName()

            name shouldNotBe null
            if (name != null) {
                name shouldBe "Ola Veldig Normann"
            }
        }

        it("Should set correct space between mame without middle name") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("Ola", null, "Normann")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )

            val name = pdlResponse.data!!.toPersonName()

            name shouldNotBe null
            if (name != null) {
                name shouldBe "Ola Normann"
            }
        }

        it("Should return null if name is null") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = null,
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )

            val name = pdlResponse.data!!.toPersonName()

            name shouldBe null
        }

        it("Should return null if name fields are null") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn(null, null, null)),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )

            val name = pdlResponse.data!!.toPersonName()

            name shouldBe null
        }

        it("Should return null if name fields are empty") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )
            val name = pdlResponse.data!!.toPersonName()

            name shouldBe null
        }

        it("Shoud be not gradert if adressebeskyttelse is empty") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )
            val gradering = pdlResponse.data!!.isNotGradert()

            gradering shouldBe true
        }

        it("Shoud be not gradert if adressebeskyttelse is not empty and Gradering is UGRADERT") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(Adressebeskyttelse(Gradering.UGRADERT)),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )
            val gradering = pdlResponse.data!!.isNotGradert()

            gradering shouldBe true
        }

        it("Shoud be gradert if adressebeskyttelse is not empty and Gradering is not UGRADERT") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG)),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("", "", "", ""))),
                    )
                )
            )
            val gradering = pdlResponse.data!!.isNotGradert()

            gradering shouldBe false
        }
    }
})
