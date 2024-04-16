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
    val pageNumber: String? = "1",
    val resultsPerPage: String? = "1",
)

data class SearchRule(
    val name: String? = "equals",
    val value: String,
)

data class Criterion(
    val fieldName: String? = "vegadresse.postnummer",
    val searchRule: SearchRule,
)
