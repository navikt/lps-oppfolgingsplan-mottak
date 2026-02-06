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
            val vegadresse = personInfo.hentPerson?.bostedsadresse?.firstOrNull()?.vegadresse
            val bosted: String

            return if (vegadresse != null && !vegadresse.postnummer.isNullOrEmpty()) {
                bosted = pdlClient.getPoststed(vegadresse.postnummer) ?: ""

                "${vegadresse.adressenavn ?: ""} ${vegadresse.husnummer ?: ""}" +
                    "${vegadresse.husbokstav ?: ""} ${vegadresse.postnummer} $bosted"
            } else {
                log.info("Can not get person's address string due to missing bostedsadresse/vegadresse/postnummer")
                null
            }
        } else {
            log.info("Can not get person's address string due to adressebeskyttelse")
            return null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PdlUtils::class.qualifiedName)
    }
}
