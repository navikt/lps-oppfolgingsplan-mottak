package no.nav.syfo.consumer.pdl

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlIdentResponse(
    val errors: List<PdlError>?,
    val data: PdlHentIdenter?
)

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlIdenter(
    val identer: List<PdlIdent>
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlIdent(
    val ident: String
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlPersonResponse(
    val errors: List<PdlError>?,
    val data: PdlHentPerson?
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlHentPerson(
    val hentPerson: PdlPerson?
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>?,
    val navn: List<PersonNavn>?,
    val foedsel: List<PdlFoedsel>?
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlFoedsel(val foedselsdato: String?)

@Suppress("SerialVersionUIDInSerializableClass")
data class PersonNavn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

@Suppress("SerialVersionUIDInSerializableClass")
data class Adressebeskyttelse(
    val gradering: Gradering
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
enum class Gradering : Serializable {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG
}

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

@Suppress("SerialVersionUIDInSerializableClass")
data class PdlErrorExtension(
    val code: String?,
    val classification: String
)

enum class IdentType {
    FOLKEREGISTERIDENT
}
