package no.nav.syfo.client.pdfgen

import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.isNotGradert
import no.nav.syfo.client.pdl.domain.toPersonName
import org.slf4j.LoggerFactory

class PdlUtils(
    private val pdlClient: PdlClient,
) {
    fun getPersonNameString(
        pdlPerson: PdlHentPerson?,
        fnr: String,
    ): String = pdlPerson?.toPersonName() ?: fnr

    suspend fun getPersonAdressString(fnr: String): String? {
        val personInfo = pdlClient.getPersonInfo(fnr)
        if (personInfo == null || !personInfo.isNotGradert()) {
            log.info("Can not get person's address string due to adressebeskyttelse")
            return null
        }

        val vegadresse =
            personInfo.hentPerson
                ?.bostedsadresse
                ?.firstOrNull()
                ?.vegadresse
        val postnummer = vegadresse?.postnummer
        if (postnummer.isNullOrEmpty()) {
            log.info("Can not get person's address string due to missing bostedsadresse/vegadresse/postnummer")
            return null
        }

        val poststed = pdlClient.getPoststed(postnummer).orEmpty()
        with(vegadresse) {
            return "${adressenavn.orEmpty()} ${husnummer.orEmpty()}${husbokstav.orEmpty()} $postnummer $poststed"
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PdlUtils::class.qualifiedName)
    }
}
