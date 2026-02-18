package no.nav.syfo.client.ereg.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonResponse(
    val navn: EregOrganisasjonNavn,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
    val sammensattnavn: String,
)

fun EregOrganisasjonResponse.getNavn(): String = this.navn.sammensattnavn
