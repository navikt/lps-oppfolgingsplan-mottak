package no.nav.syfo.client.pdl.domain

interface PdlRequestInterface

data class PdlRequest(
    val query: String,
    val variables: Variables,
) : PdlRequestInterface

data class Variables(
    val ident: String,
)

data class SokAdressePdlRequest(
    val query: String,
    val variables: SokAdresseVariables,
) : PdlRequestInterface

data class SokAdresseVariables(
    val paging: Paging,
    val criteria: List<Criterion>,
)

data class Paging(
    val pageNumber: Int? = 1,
    val resultsPerPage: Int? = 1,
)

data class SearchRule(
    val equals: String,
)

data class Criterion(
    val fieldName: String? = "vegadresse.postnummer",
    val searchRule: SearchRule,
)
