package no.nav.syfo.client.pdl.domain

import org.slf4j.LoggerFactory
import java.io.Serializable

data class PdlIdenterResponse(
    val errors: List<PdlError>?,
    val data: PdlHentIdenter?,
)

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?,
) : Serializable

data class PdlIdenter(
    val identer: List<PdlIdent>,
) : Serializable

data class PdlIdent(
    val ident: String,
) : Serializable

data class PdlPersonResponse(
    val errors: List<PdlError>?,
    val data: PdlHentPerson?,
) : Serializable

data class PdlHentPerson(
    val hentPerson: PdlPerson?,
) : Serializable

data class PdlPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>?,
    val navn: List<PersonNavn>?,
    val bostedsadresse: List<Bostedsadresse>,
) : Serializable

data class Bostedsadresse(
    val vegadresse: Vegadresse?,
)

data class Vegadresse(
    val adressenavn: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val bruksenhetsnummer: String?,
    val postnummer: String?,
)

data class PersonNavn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class Adressebeskyttelse(
    val gradering: Gradering,
) : Serializable

enum class Gradering : Serializable {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT, // Ugraderte personer kan også ha tomt felt
}

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension,
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?,
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String,
)

fun PdlHentPerson.toPersonName(): String? {
    val navn = this.hentPerson?.navn?.first()

    return if (navn == null) {
        null
    } else {
        "${navn.fornavn}${getMellomnavn(navn.mellomnavn)} ${navn.etternavn}"
    }
}

private fun getMellomnavn(mellomnavn: String?): String {
    return if (mellomnavn !== null) " $mellomnavn" else ""
}

fun PdlHentPerson.toPersonAdresse(): String? {
    log.warn("zxzx: is gradert ${this.isGradert()}")
    if (!this.isGradert()) {
        val vegadresse = this.hentPerson?.bostedsadresse?.first()?.vegadresse
        log.warn("zxzx: vegadresse ${vegadresse}")
        if (vegadresse != null) {
            log.warn("zxzx: adresse str ${vegadresse.adressenavn} ${vegadresse.husnummer}${vegadresse.husbokstav}, ${vegadresse.postnummer}")

            return "${vegadresse.adressenavn} ${vegadresse.husnummer}${vegadresse.husbokstav}, ${vegadresse.postnummer}"
        }
    }
    log.info("Can not get person's address due to adressebeskyttelse")
    return null
}

fun PdlHentPerson.isGradert(): Boolean {
    val adressebeskyttelse = this.hentPerson?.adressebeskyttelse
    log.warn("zxzx: adressebeskyttelse ${adressebeskyttelse}")

    return adressebeskyttelse.isNullOrEmpty() || (adressebeskyttelse.first().gradering.name.isNotEmpty() && adressebeskyttelse.first().gradering.name != Gradering.UGRADERT.name)
}

private val log = LoggerFactory.getLogger(PdlHentPerson::class.qualifiedName)
