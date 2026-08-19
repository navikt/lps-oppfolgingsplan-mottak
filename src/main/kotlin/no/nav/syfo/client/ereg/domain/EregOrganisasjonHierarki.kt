package no.nav.syfo.client.ereg.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

/**
 * Modell for ereg-responsen `GET /ereg/api/v2/organisasjon/{orgnr}?inkluderHierarki=true`.
 *
 * aaregs arbeidsforholdoversikt er to-nivå (arbeidssted + opplysningspliktig/juridisk enhet) og
 * utelater eventuelle organisasjonsledd som ligger imellom. For offentlige arbeidsgivere kan
 * Maskinporten-tokenet være utstedt på et slikt organisasjonsledd. Denne modellen lar oss slå opp
 * arbeidsstedets fulle hierarki og avgjøre om et oppgitt orgnummer er en gyldig overordnet enhet.
 *
 * Hver relasjon har en [EregGyldighetsperiode]; kun relasjoner som er gyldige i dag tas med, slik at
 * et historisk (avsluttet) tilhørighetsforhold ikke gir tilgang.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjon(
    val organisasjonsnummer: String,
    val inngaarIJuridiskEnheter: List<EregEnhetsRelasjon>? = null,
    val bestaarAvOrganisasjonsledd: List<EregOrganisasjonsleddWrapper>? = null,
) {
    /**
     * Samler alle organisasjonsnummer i hierarkiet over (og inkludert) denne enheten:
     * enheten selv, juridiske enheter den inngår i, og organisasjonsledd den består av (rekursivt).
     * Utløpte relasjoner ekskluderes.
     */
    fun aggregerOrgnummereFraHierarki(): Set<String> {
        val orgnummere = mutableSetOf(organisasjonsnummer)
        inngaarIJuridiskEnheter
            ?.filter { it.gyldighetsperiode.erGyldigNaa() }
            ?.mapTo(orgnummere) { it.organisasjonsnummer }
        bestaarAvOrganisasjonsledd
            ?.filter { it.gyldighetsperiode.erGyldigNaa() }
            ?.forEach { orgnummere.addAll(it.organisasjonsledd.collectOrgnummer()) }
        return orgnummere
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonsleddWrapper(
    val organisasjonsledd: EregOrganisasjonsledd,
    val gyldighetsperiode: EregGyldighetsperiode? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonsledd(
    val organisasjonsnummer: String,
    val inngaarIJuridiskEnheter: List<EregEnhetsRelasjon>? = null,
    val organisasjonsleddOver: List<EregOrganisasjonsleddWrapper>? = null,
) {
    fun collectOrgnummer(visited: MutableSet<String> = mutableSetOf()): Set<String> {
        if (!visited.add(organisasjonsnummer)) {
            return emptySet()
        }
        val orgnummere = mutableSetOf(organisasjonsnummer)
        inngaarIJuridiskEnheter
            ?.filter { it.gyldighetsperiode.erGyldigNaa() }
            ?.mapTo(orgnummere) { it.organisasjonsnummer }
        organisasjonsleddOver
            ?.filter { it.gyldighetsperiode.erGyldigNaa() }
            ?.forEach { orgnummere.addAll(it.organisasjonsledd.collectOrgnummer(visited)) }
        return orgnummere
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregEnhetsRelasjon(
    val organisasjonsnummer: String,
    val gyldighetsperiode: EregGyldighetsperiode? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregGyldighetsperiode(
    val fom: String? = null,
    val tom: String? = null,
) {
    fun erGyldig(idag: LocalDate = LocalDate.now()): Boolean {
        val startet = fom?.let { !parseDato(it).isAfter(idag) } ?: true
        val ikkeUtloept = tom?.let { !parseDato(it).isBefore(idag) } ?: true
        return startet && ikkeUtloept
    }

    private fun parseDato(verdi: String): LocalDate = LocalDate.parse(verdi.take(10))
}

/** En manglende gyldighetsperiode regnes som gyldig (fail-closed kun på eksplisitt utløpte relasjoner). */
fun EregGyldighetsperiode?.erGyldigNaa(): Boolean = this?.erGyldig() ?: true
