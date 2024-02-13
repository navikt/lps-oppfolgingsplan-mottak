package no.nav.syfo.application.api.auth

@Suppress("ConstructorParameterNaming", "MatchingDeclarationName")
data class Consumer(
    val authority: String,
    val ID: String
)

fun Consumer.getOrgnumber(): String {
    return ID.split(":")[1]
}
