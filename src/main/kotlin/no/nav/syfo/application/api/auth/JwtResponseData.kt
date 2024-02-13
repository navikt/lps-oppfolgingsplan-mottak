package no.nav.syfo.application.api.auth

@Suppress("ConstructorParameterNaming", "MatchingDeclarationName")
data class IdClaim(
    val authority: String,
    val ID: String
)

fun IdClaim.getOrgnumber(): String {
    return ID.split(":")[1]
}
