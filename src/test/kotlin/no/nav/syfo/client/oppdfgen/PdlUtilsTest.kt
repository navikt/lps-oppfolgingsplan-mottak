package no.nav.syfo.client.oppdfgen

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.syfo.application.exception.PdlNotFoundException
import no.nav.syfo.application.exception.PdlServerException
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.domain.Adressebeskyttelse
import no.nav.syfo.client.pdl.domain.Bostedsadresse
import no.nav.syfo.client.pdl.domain.Gradering
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.PdlPerson
import no.nav.syfo.client.pdl.domain.PersonNavn
import no.nav.syfo.client.pdl.domain.Vegadresse

class PdlUtilsTest : DescribeSpec({
    val pdlClient = mockk<PdlClient>()
    val pdlUtils = PdlUtils(pdlClient)
    beforeTest {
        clearAllMocks()
    }

    describe("Validation of adress string") {

        it("Should set correct spaces in address string if all fields are present") {
            coEvery { pdlClient.getPersonInfo(any()) } returns PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(
                        Adressebeskyttelse(gradering = Gradering.UGRADERT)
                    ),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "A", postnummer = "1234")))
                )
            )

            coEvery { pdlClient.getPoststed(any()) } returns "OSLO"

            val address = pdlUtils.getPersonAdressString("11111111111")

            address shouldBe "Fancy gate 1A 1234 OSLO"
        }

        it("Should set correct spaces in address string if all husbokstav field is missing") {

            coEvery { pdlClient.getPersonInfo(any()) } returns PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(
                        Adressebeskyttelse(gradering = Gradering.UGRADERT)
                    ),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "", postnummer = "1234")))
                )
            )
            coEvery { pdlClient.getPoststed(any()) } returns "OSLO"

            val address = pdlUtils.getPersonAdressString("11111111111")

            address shouldBe "Fancy gate 1 1234 OSLO"
        }

        it("Should return null if postnummer is missing") {
            coEvery { pdlClient.getPersonInfo(any()) } returns PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(
                        Adressebeskyttelse(gradering = Gradering.UGRADERT)
                    ),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "A", postnummer = "")))
                )
            )
            coEvery { pdlClient.getPoststed(any()) } returns "OSLO"

            val address = pdlUtils.getPersonAdressString("11111111111")

            address shouldBe null
        }

        it("Should return null if postnummer is missing and bosted is missing") {
            coEvery { pdlClient.getPersonInfo(any()) } returns PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(
                        Adressebeskyttelse(gradering = Gradering.UGRADERT)
                    ),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "A", postnummer = "1234")))
                )
            )
            coEvery { pdlClient.getPoststed(any()) } returns null

            val address = pdlUtils.getPersonAdressString("11111111111")

            address shouldBe "Fancy gate 1A 1234 "
        }

        it("Should return null if person is gradert") {
            coEvery { pdlClient.getPersonInfo(any()) } returns PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(
                        Adressebeskyttelse(gradering = Gradering.STRENGT_FORTROLIG)
                    ),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    listOf(Bostedsadresse(Vegadresse("Fancy gate", "1", "A", postnummer = "1234")))
                )
            )
            coEvery { pdlClient.getPoststed(any()) } returns null

            val address = pdlUtils.getPersonAdressString("11111111111")
            address shouldBe null
        }
    }

    describe("getPersonNameString") {
        it("Should return person name when available") {
            val pdlPerson = PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(Adressebeskyttelse(gradering = Gradering.UGRADERT)),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    emptyList()
                )
            )

            val result = pdlUtils.getPersonNameString(pdlPerson, "12345678901")

            result shouldBe "Ola Veldig Nordmann"
        }

        it("Should return FNR when name is not available") {
            val fnr = "12345678901"

            val result = pdlUtils.getPersonNameString(null, fnr)

            result shouldBe fnr
        }
    }

    describe("getPersonInfoWithRetry") {
        it("Should return person info on first successful attempt") {
            val fnr = "12345678901"
            val pdlPerson = PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(Adressebeskyttelse(gradering = Gradering.UGRADERT)),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    emptyList()
                )
            )
            coEvery { pdlClient.getPersonInfo(fnr) } returns pdlPerson

            val result = pdlUtils.getPersonInfoWithRetry(fnr, 3)

            result shouldBe pdlPerson
            coVerify(exactly = 1) { pdlClient.getPersonInfo(fnr) }
        }

        it("Should retry on server error and succeed on second attempt") {
            val fnr = "12345678901"
            val pdlPerson = PdlHentPerson(
                PdlPerson(
                    adressebeskyttelse = listOf(Adressebeskyttelse(gradering = Gradering.UGRADERT)),
                    listOf(PersonNavn("Ola", "Veldig", "Nordmann")),
                    emptyList()
                )
            )
            coEvery { pdlClient.getPersonInfo(fnr) } throws PdlServerException("Server error") andThen pdlPerson

            val result = pdlUtils.getPersonInfoWithRetry(fnr, 3)

            result shouldBe pdlPerson
            coVerify(exactly = 2) { pdlClient.getPersonInfo(fnr) }
        }

        it("Should not retry on client errors like PdlNotFoundException") {
            val fnr = "12345678901"
            coEvery { pdlClient.getPersonInfo(fnr) } throws PdlNotFoundException("Not found")

            val result = pdlUtils.getPersonInfoWithRetry(fnr, 3)

            result shouldBe null
            coVerify(exactly = 1) { pdlClient.getPersonInfo(fnr) }
        }

        it("Should return null after exhausting all retries") {
            val fnr = "12345678901"
            coEvery { pdlClient.getPersonInfo(fnr) } throws PdlServerException("Server error")

            val result = pdlUtils.getPersonInfoWithRetry(fnr, 3)

            result shouldBe null
            coVerify(exactly = 3) { pdlClient.getPersonInfo(fnr) }
        }
    }
})
