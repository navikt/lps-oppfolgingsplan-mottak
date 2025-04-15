package no.nav.syfo.client.oppdfgen

import no.nav.syfo.application.exception.PdlException
import no.nav.syfo.application.exception.PdlServerException
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.isNotGradert
import no.nav.syfo.client.pdl.domain.toPersonName
import org.slf4j.LoggerFactory

class PdlUtils(private val pdlClient: PdlClient) {
    private val log = LoggerFactory.getLogger(PdlUtils::class.qualifiedName)

    fun getPersonNameString(pdlPerson: PdlHentPerson?, fnr: String): String {
        return pdlPerson?.toPersonName() ?: fnr
    }

    suspend fun getPersonAdressString(fnr: String): String? {
        val personInfo = getPersonInfoWithRetry(fnr) ?: return null

        if (personInfo != null && personInfo.isNotGradert()) {
            val vegadresse = personInfo.hentPerson?.bostedsadresse?.first()?.vegadresse
            val bosted: String

            return if (vegadresse != null && !vegadresse.postnummer.isNullOrEmpty()) {
                bosted = pdlClient.getPoststed(vegadresse.postnummer) ?: ""

                "${vegadresse.adressenavn ?: ""} ${vegadresse.husnummer ?: ""}" +
                        "${vegadresse.husbokstav ?: ""} ${vegadresse.postnummer} $bosted"
            } else {
                log.info("Can not get person's address string due to vegadresse or postnummer are null")
                null
            }
        } else {
            log.info("Can not get person's address string due to adressebeskyttelse")
            return null
        }
    }

    suspend fun getPersonInfoWithRetry(fnr: String, maxRetries: Int = 3): PdlHentPerson? {
        var retries = 0
        var lastError: Exception? = null

        while (retries < maxRetries) {
            try {
                return pdlClient.getPersonInfo(fnr)
            } catch (e: PdlServerException) {
                // Only retry on server errors
                lastError = e
                retries++
                if (retries < maxRetries) {
                    log.info("Retrying getPersonInfo after server error (attempt $retries/$maxRetries)")
                    kotlinx.coroutines.delay(1000L * retries) // Exponential backoff
                }
            } catch (e: PdlException) {
                log.error("Failed to get person info: ${e.message}", e)
                return null
            } catch (e: Exception) {
                log.error("Unexpected error in getPersonInfoWithRetry: ${e.message}", e)
                return null
            }
        }

        log.error("Failed to get person info after $maxRetries retries", lastError)
        return null
    }
}
