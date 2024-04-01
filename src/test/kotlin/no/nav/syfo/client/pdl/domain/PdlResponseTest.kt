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

        it("Should set correct spaces in address string if all fields are present") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "A", "1234"))),
                    )
                )
            )
            val address = pdlResponse.data!!.toPersonAdress()

            address shouldBe "Fancy gate 1A, 1234"
        }

        it("Should set correct spaces in address string if all husbokstav field is missing") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "", "1234"))),
                    )
                )
            )
            val address = pdlResponse.data!!.toPersonAdress()

            address shouldBe "Fancy gate 1, 1234"
        }

        it("Should not set comma and extra space in address string if postnummer is missing") {
            val pdlResponse = PdlPersonResponse(
                null,
                PdlHentPerson(
                    PdlPerson(
                        adressebeskyttelse = listOf(),
                        navn = listOf(PersonNavn("", "", "")),
                        bostedsadresse = listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "B", ""))),
                    )
                )
            )
            val address = pdlResponse.data!!.toPersonAdress()

            address shouldBe "Fancy gate 1B"
        }
    }

})
