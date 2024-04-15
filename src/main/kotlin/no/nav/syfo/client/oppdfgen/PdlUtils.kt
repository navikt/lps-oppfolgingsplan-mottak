package no.nav.syfo.client.oppdfgen

import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.isNotGradert
import no.nav.syfo.client.pdl.domain.toPersonName
import org.slf4j.LoggerFactory

class PdlUtils(private val pdlClient: PdlClient) {

    fun getPersonNameString(pdlPerson: PdlHentPerson?, fnr: String): String {
        return pdlPerson?.toPersonName() ?: fnr
    }

    suspend fun getPersonAdressString(fnr: String): String? {
        val personInfo = pdlClient.getPersonInfo(fnr)

        if (personInfo != null && personInfo.isNotGradert()) {
            val vegadresse = personInfo.hentPerson?.bostedsadresse?.first()?.vegadresse
            val postnummer: String
            val bosted: String
            val adressenavn: String
            val husnummer: String
            val husbokstav: String

            if (vegadresse != null) {
                return if (!vegadresse.postnummer.isNullOrEmpty()) {
                    log.info("QWQW vegadresse.postnummer: ${vegadresse.postnummer}")
                    adressenavn = vegadresse.adressenavn ?: ""
                    husnummer = vegadresse.husnummer ?: ""
                    husbokstav = vegadresse.husbokstav ?: ""
                    postnummer = vegadresse.postnummer
                    bosted = pdlClient.getPoststed(postnummer) ?: ""

                    "$adressenavn ${husnummer}${husbokstav} $postnummer $bosted"
                } else {
                    log.info("QWQW Can not get person's postnummer due to postnummer is null or empty")
                   null
                }
            } else {
                log.info("QWQW Can not get person's postnummer due to vegadresse is null")
                return null
            }
        } else {
            log.info("QWW Can not get person's address due to adressebeskyttelse")
            return null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PdlUtils::class.qualifiedName)
    }
}
