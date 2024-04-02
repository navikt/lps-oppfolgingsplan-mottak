package no.nav.syfo.client.ereg.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonResponse(
        val navn: EregOrganisasjonNavn
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
        val navnelinje1: String,
        val redigertnavn: String?
)

fun EregOrganisasjonResponse.getNavn(): String {
    return this.navn.let {
        if (it.redigertnavn?.isNotEmpty() == true) {
            it.redigertnavn
        } else {
            it.navnelinje1
        }
    }
}
