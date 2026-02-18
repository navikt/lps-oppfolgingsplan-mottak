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
    var adressebeskyttelse: List<Adressebeskyttelse>?,
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
    val locations: List<PdlErrorLocation>?,
    val path: List<String>?,
    val extensions: PdlErrorExtension,
)

data class PdlSokAdresseResponse(
    val errors: List<PdlError>?,
    val data: PdlSokAdresse?,
) : Serializable

data class PdlSokAdresse(
    val sokAdresse: SokAdresse?,
) : Serializable

data class SokAdresse(
    val hits: List<Hit>,
) : Serializable

data class Hit(
    val vegadresse: Poststed?,
) : Serializable

data class Poststed(
    val poststed: String?,
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

    return if (navn?.fornavn.isNullOrEmpty() || navn?.etternavn.isNullOrEmpty()) {
        null
    } else {
        "${navn?.fornavn}${getMellomnavn(navn?.mellomnavn)} ${navn?.etternavn}"
    }
}

private fun getMellomnavn(mellomnavn: String?): String = if (mellomnavn !== null) " $mellomnavn" else ""

fun PdlHentPerson.isNotGradert(): Boolean {
    val graderingName =
        this.hentPerson
            ?.adressebeskyttelse
            ?.firstOrNull()
            ?.gradering
            ?.name
    return graderingName == null || graderingName == Gradering.UGRADERT.name
}
