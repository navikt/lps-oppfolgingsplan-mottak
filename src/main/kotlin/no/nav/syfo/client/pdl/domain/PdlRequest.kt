package no.nav.syfo.client.pdl.domain

data class PdlRequest(
    val query: String,
    val variables: Variables
)

data class Variables(
    val ident: String,
    val grupper: String = IdentType.FOLKEREGISTERIDENT.name,
    val historikk: Boolean = false
)

enum class IdentType {
    FOLKEREGISTERIDENT
}
