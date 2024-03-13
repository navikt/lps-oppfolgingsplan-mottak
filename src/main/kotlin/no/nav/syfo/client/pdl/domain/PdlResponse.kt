package no.nav.syfo.client.pdl.domain

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
    val telefonnummer: List<Telefonnummer>?,
) : Serializable

data class Telefonnummer(
    val landskode: String?,
    val nummer: String?,
    val prioritet: String?,
)

data class Bostedsadresse(
    val vegadresse: Vegadresse?,
)

data class Vegadresse(
    val adressenavn: String,
    val husnummer: String,
    val husbokstav: String,
    val bruksenhetsnummer: String,
    val postnummer: String,
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
    FORTROLIG
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

fun PdlHentPerson.toPersonPhoneNumber(): String?{
    val  preferredPhoneNumber = this.hentPerson?.telefonnummer?.filter { it.prioritet == "1" }?.first()
    if (preferredPhoneNumber?.nummer != null){
        return "${preferredPhoneNumber.landskode} ${preferredPhoneNumber.nummer}"
    }
    return null
}

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

fun PdlHentPerson.toPersonAdresse(): String?{
//    val graderingValues = Gradering.entries.map { it.name } // TODO
//    val adressebeskyttelse = graderingValues.contains(this.hentPerson?.adressebeskyttelse?.first()?.gradering?.name)
    val  vegadresse = this.hentPerson?.bostedsadresse?.first()?.vegadresse

    if (vegadresse != null){
//    if (!adressebeskyttelse && vegadresse != null){
        return "${vegadresse.adressenavn} ${vegadresse.husnummer}${vegadresse.husbokstav}, ${vegadresse.postnummer}"
    }
    return null
}