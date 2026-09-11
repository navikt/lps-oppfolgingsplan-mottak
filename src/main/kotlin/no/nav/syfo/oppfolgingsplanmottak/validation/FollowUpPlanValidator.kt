package no.nav.syfo.oppfolgingsplanmottak.validation

import no.nav.syfo.application.exception.EmployeeNotFoundException
import no.nav.syfo.application.exception.FollowUpPlanDTOValidationException
import no.nav.syfo.application.exception.NoActiveEmploymentException
import no.nav.syfo.application.exception.NoActiveSentSykmeldingException
import no.nav.syfo.client.aareg.ArbeidsforholdOversiktClient
import no.nav.syfo.client.aareg.domain.Arbeidsforholdoversikt
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import org.slf4j.LoggerFactory

val TEST_FNR_LIST = listOf("05908399546", "01898299631")

class FollowUpPlanValidator(
    private val pdlClient: PdlClient,
    private val sykmeldingService: SendtSykmeldingService,
    private val arbeidsforholdOversiktClient: ArbeidsforholdOversiktClient,
    private val eregClient: EregClient,
    private val isDev: Boolean,
) {
    private val log = LoggerFactory.getLogger(FollowUpPlanValidator::class.java)

    suspend fun validateFollowUpPlanDTO(
        followUpPlanDTO: FollowUpPlanDTO,
        employerOrgnr: String,
    ) {
        validatePlan(followUpPlanDTO)

        validateEmployeeInformation(followUpPlanDTO, employerOrgnr, isDev)
    }

    private fun validatePlan(followUpPlanDTO: FollowUpPlanDTO) {
        if (followUpPlanDTO.needsHelpFromNav == true && !followUpPlanDTO.sendPlanToNav) {
            throw FollowUpPlanDTOValidationException("needsHelpFromNav cannot be true if sendPlanToNav is false")
        }
        if (followUpPlanDTO.needsHelpFromNav == true && followUpPlanDTO.needsHelpFromNavDescription.isNullOrBlank()) {
            throw FollowUpPlanDTOValidationException(
                "needsHelpFromNavDescription is obligatory if needsHelpFromNav is true",
            )
        }
        if (!followUpPlanDTO.employeeHasContributedToPlan &&
            followUpPlanDTO.employeeHasNotContributedToPlanDescription.isNullOrBlank()
        ) {
            throw FollowUpPlanDTOValidationException(
                "employeeHasNotContributedToPlanDescription is mandatory if employeeHasContributedToPlan = false",
            )
        }
        if (followUpPlanDTO.employeeHasContributedToPlan &&
            followUpPlanDTO.employeeHasNotContributedToPlanDescription?.isNotEmpty() == true
        ) {
            throw FollowUpPlanDTOValidationException(
                "employeeHasNotContributedToPlanDescription should not be set if employeeHasContributedToPlan = true",
            )
        }
    }

    private suspend fun validateEmployeeInformation(
        followUpPlanDTO: FollowUpPlanDTO,
        employerOrgnr: String,
        isDev: Boolean,
    ) {
        if (!followUpPlanDTO.employeeIdentificationNumber.matches(Regex("\\d{11}"))) {
            throw FollowUpPlanDTOValidationException("Invalid employee identification number")
        }

        if (isDev && TEST_FNR_LIST.contains(followUpPlanDTO.employeeIdentificationNumber)) {
            return
        }

        val validOrgnumbers = validateArbeidsforhold(followUpPlanDTO, employerOrgnr)

        validateSykmelding(followUpPlanDTO, validOrgnumbers)

        if (pdlClient.getPersonInfo(followUpPlanDTO.employeeIdentificationNumber) == null) {
            throw EmployeeNotFoundException("Could not find requested person in our systems")
        }
    }

    private fun validateSykmelding(
        followUpPlanDTO: FollowUpPlanDTO,
        validOrgnumbers: List<String?>,
    ) {
        val activeSykmeldinger: List<Sykmeldingsperiode> =
            sykmeldingService.getActiveSendtSykmeldingsperioder(
                followUpPlanDTO.employeeIdentificationNumber,
            )

        val hasActiveSendtSykmelding = activeSykmeldinger.any { validOrgnumbers.contains(it.organizationNumber) }
        if (!hasActiveSendtSykmelding) {
            throw NoActiveSentSykmeldingException("No active sykmelding sent to employer")
        }
    }

    private suspend fun validateArbeidsforhold(
        followUpPlanDTO: FollowUpPlanDTO,
        employerOrgnr: String,
    ): List<String?> {
        val arbeidsforhold =
            arbeidsforholdOversiktClient
                .getArbeidsforhold(followUpPlanDTO.employeeIdentificationNumber)
                ?.arbeidsforholdoversikter
                ?: emptyList()

        val matchingArbeidsforhold = findMatchingArbeidsforhold(arbeidsforhold, employerOrgnr)

        if (matchingArbeidsforhold.isEmpty()) {
            arbeidsforhold.forEach {
                log.info(
                    "Found arbeidsforhold in orgnumber:" +
                        " ${it.opplysningspliktig.getJuridiskOrgnummer()} and ${it.arbeidssted.getOrgnummer()}",
                )
            }
            throw NoActiveEmploymentException(
                "No active employment relationship found for given orgnumber: $employerOrgnr",
            )
        }

        return matchingArbeidsforhold
            .flatMap {
                listOf(
                    it.opplysningspliktig.getJuridiskOrgnummer(),
                    it.arbeidssted.getOrgnummer(),
                )
            }.distinct()
    }

    /**
     * Finner arbeidsforhold som hører til [employerOrgnr]. Først direkte match mot juridisk enhet
     * (opplysningspliktig) eller arbeidssted (underenhet). Hvis ingen direkte match: slår opp
     * arbeidsstedets hierarki i ereg og godtar arbeidsforhold der [employerOrgnr] er en gyldig
     * overordnet enhet — typisk et organisasjonsledd som ligger mellom underenhet og juridisk enhet,
     * og som ikke finnes i aaregs to-nivå-modell.
     */
    private suspend fun findMatchingArbeidsforhold(
        arbeidsforhold: List<Arbeidsforholdoversikt>,
        employerOrgnr: String,
    ): List<Arbeidsforholdoversikt> {
        val directMatches =
            arbeidsforhold.filter {
                it.opplysningspliktig.getJuridiskOrgnummer() == employerOrgnr ||
                    it.arbeidssted.getOrgnummer() == employerOrgnr
            }
        if (directMatches.isNotEmpty()) {
            return directMatches
        }

        // Ett ereg-oppslag per unikt arbeidssted (ikke per arbeidsforhold) for å unngå duplikate kall.
        val matchendeArbeidssteder = mutableSetOf<String>()
        for (arbeidsstedOrgnr in arbeidsforhold.mapNotNull { it.arbeidssted.getOrgnummer() }.distinct()) {
            val hierarkiOrgnumre =
                eregClient.getOrganisasjonHierarki(arbeidsstedOrgnr)?.aggregerOrgnummereFraHierarki().orEmpty()
            if (employerOrgnr in hierarkiOrgnumre) {
                log.info(
                    "Matched employer $employerOrgnr as overordnet enhet for arbeidssted" +
                        " $arbeidsstedOrgnr via ereg hierarki",
                )
                matchendeArbeidssteder.add(arbeidsstedOrgnr)
            }
        }
        return arbeidsforhold.filter { it.arbeidssted.getOrgnummer() in matchendeArbeidssteder }
    }
}
