package no.nav.syfo.oppfolgingsplanmottak.validation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.application.exception.FollowUpPlanDTOValidationException
import no.nav.syfo.application.exception.NoActiveEmploymentException
import no.nav.syfo.application.exception.NoActiveSentSykmeldingException
import no.nav.syfo.client.aareg.ArbeidsforholdOversiktClient
import no.nav.syfo.client.aareg.domain.AaregArbeidsforholdOversikt
import no.nav.syfo.client.aareg.domain.Arbeidsforholdoversikt
import no.nav.syfo.client.aareg.domain.Arbeidssted
import no.nav.syfo.client.aareg.domain.ArbeidsstedType
import no.nav.syfo.client.aareg.domain.Ident
import no.nav.syfo.client.aareg.domain.IdentType
import no.nav.syfo.client.aareg.domain.Opplysningspliktig
import no.nav.syfo.client.aareg.domain.OpplysningspliktigType
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.oppfolgingsplanmottak.domain.FollowUpPlanDTO
import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

const val UNDERENHET_ORGNUMBER = "123456789"
const val HOVEDENHET_ORGNUMBER = "987654321"
const val OTHER_COMPANY_UNDERENHET_ORGNUMBER = "234567890"
const val OTHER_COMPANY_HOVEDENHET_ORGNUMBER = "876543219"
const val EMPLOYEE_SSN = "12345678901"

class FollowUpPlanValidatorTest : DescribeSpec({
    val pdlClient = mockk<PdlClient>()
    val sykmeldingService = mockk<SendtSykmeldingService>()
    val arbeidsforholdOversiktClient = mockk<ArbeidsforholdOversiktClient>()
    val validator = FollowUpPlanValidator(pdlClient, sykmeldingService, arbeidsforholdOversiktClient, false)

    describe("FollowUpPlanValidator") {
        context("validateFollowUpPlanDTO") {
            it("should throw exception if needsHelpFromNav is true and sendPlanToNav is false") {
                val followUpPlanDTO = createFollowUpPlanDTO(needsHelpFromNav = true, sendPlanToNav = false)
                shouldThrow<FollowUpPlanDTOValidationException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it("should throw exception if needsHelpFromNav is true and needsHelpFromNavDescription is null") {
                val followUpPlanDTO = createFollowUpPlanDTO(needsHelpFromNav = true, needsHelpFromNavDescription = null)
                shouldThrow<FollowUpPlanDTOValidationException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it(
                "should throw exception if employeeHasContributedToPlan is false " +
                    "and employeeHasNotContributedToPlanDescription is null"
            ) {
                val followUpPlanDTO = createFollowUpPlanDTO(
                    employeeHasContributedToPlan = false,
                    employeeHasNotContributedToPlanDescription = null
                )
                shouldThrow<FollowUpPlanDTOValidationException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it(
                "should throw exception if employeeHasContributedToPlan is true " +
                    "and employeeHasNotContributedToPlanDescription is not empty"
            ) {
                val followUpPlanDTO = createFollowUpPlanDTO(
                    employeeHasContributedToPlan = true,
                    employeeHasNotContributedToPlanDescription = "Some description"
                )
                shouldThrow<FollowUpPlanDTOValidationException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it("should throw exception if employee identification number is invalid") {
                val followUpPlanDTO = createFollowUpPlanDTO(employeeIdentificationNumber = "invalid")
                coEvery { pdlClient.getPersonInfo(any()) } returns null
                shouldThrow<FollowUpPlanDTOValidationException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it("should throw exception if no active sykmelding is found") {
                val followUpPlanDTO = createFollowUpPlanDTO()
                coEvery {
                    arbeidsforholdOversiktClient.getArbeidsforhold(any())
                } returns createValidAaregArbeidsforholdOversiktDTO(HOVEDENHET_ORGNUMBER, UNDERENHET_ORGNUMBER)
                coEvery { sykmeldingService.getActiveSendtSykmeldingsperioder(any()) } returns emptyList()
                coEvery { pdlClient.getPersonInfo(any()) } returns mockk()
                shouldThrow<NoActiveSentSykmeldingException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it("should throw exception if no active employment relationship is found") {
                val followUpPlanDTO = createFollowUpPlanDTO()
                coEvery { sykmeldingService.getActiveSendtSykmeldingsperioder(any()) } returns listOf(mockk())
                coEvery { arbeidsforholdOversiktClient.getArbeidsforhold(any()) } returns null
                coEvery { pdlClient.getPersonInfo(any()) } returns mockk()
                shouldThrow<NoActiveEmploymentException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it("should throw exception if works in another company") {
                val followUpPlanDTO = createFollowUpPlanDTO()
                coEvery { sykmeldingService.getActiveSendtSykmeldingsperioder(any()) } returns listOf(mockk())
                coEvery {
                    arbeidsforholdOversiktClient.getArbeidsforhold(any())
                } returns createValidAaregArbeidsforholdOversiktDTO(
                    OTHER_COMPANY_HOVEDENHET_ORGNUMBER, OTHER_COMPANY_UNDERENHET_ORGNUMBER
                )
                coEvery { pdlClient.getPersonInfo(any()) } returns mockk()
                shouldThrow<NoActiveEmploymentException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
                }
            }

            it(
                "should pass validation for valid FollowUpPlanDTO, sykmelding and " +
                    "arbeidsforhold if logged in with related hovedenhet"
            ) {
                val followUpPlanDTO = createFollowUpPlanDTO()
                coEvery {
                    sykmeldingService.getActiveSendtSykmeldingsperioder(any())
                } returns createValidSykmeldingsperioder()

                coEvery {
                    arbeidsforholdOversiktClient.getArbeidsforhold(any())
                } returns createValidAaregArbeidsforholdOversiktDTO(HOVEDENHET_ORGNUMBER, UNDERENHET_ORGNUMBER)

                coEvery {
                    pdlClient.getPersonInfo(any())
                } returns mockk()

                validator.validateFollowUpPlanDTO(followUpPlanDTO, HOVEDENHET_ORGNUMBER)
            }

            it(
                "should pass validation for valid FollowUpPlanDTO, sykmelding and " +
                    "arbeidsforhold if logged in with related underenhet"
            ) {
                val followUpPlanDTO = createFollowUpPlanDTO()
                coEvery {
                    sykmeldingService.getActiveSendtSykmeldingsperioder(any())
                } returns createValidSykmeldingsperioder()

                coEvery {
                    arbeidsforholdOversiktClient.getArbeidsforhold(any())
                } returns createValidAaregArbeidsforholdOversiktDTO(HOVEDENHET_ORGNUMBER, UNDERENHET_ORGNUMBER)

                coEvery {
                    pdlClient.getPersonInfo(any())
                } returns mockk()

                validator.validateFollowUpPlanDTO(followUpPlanDTO, UNDERENHET_ORGNUMBER)
            }

            it(
                "should not pass validation for valid FollowUpPlanDTO, sykmelding and " +
                    "arbeidsforhold if logged in with some other company"
            ) {
                val followUpPlanDTO = createFollowUpPlanDTO()
                coEvery {
                    sykmeldingService.getActiveSendtSykmeldingsperioder(any())
                } returns createValidSykmeldingsperioder()

                coEvery {
                    arbeidsforholdOversiktClient.getArbeidsforhold(any())
                } returns createValidAaregArbeidsforholdOversiktDTO(HOVEDENHET_ORGNUMBER, UNDERENHET_ORGNUMBER)

                coEvery {
                    pdlClient.getPersonInfo(any())
                } returns mockk()

                shouldThrow<NoActiveEmploymentException> {
                    validator.validateFollowUpPlanDTO(followUpPlanDTO, OTHER_COMPANY_HOVEDENHET_ORGNUMBER)
                }
            }
        }
    }
})

fun createFollowUpPlanDTO(
    employeeIdentificationNumber: String = "12345678901",
    needsHelpFromNav: Boolean? = false,
    needsHelpFromNavDescription: String? = null,
    sendPlanToNav: Boolean = true,
    employeeHasContributedToPlan: Boolean = true,
    employeeHasNotContributedToPlanDescription: String? = null
): FollowUpPlanDTO {
    return FollowUpPlanDTO(
        employeeIdentificationNumber = employeeIdentificationNumber,
        typicalWorkday = "Typical workday",
        tasksThatCanStillBeDone = "Tasks that can still be done",
        tasksThatCanNotBeDone = "Tasks that cannot be done",
        previousFacilitation = "Previous facilitation",
        plannedFacilitation = "Planned facilitation",
        otherFacilitationOptions = "Other facilitation options",
        followUp = "Follow up",
        evaluationDate = LocalDate.now(),
        sendPlanToNav = sendPlanToNav,
        needsHelpFromNav = needsHelpFromNav,
        needsHelpFromNavDescription = needsHelpFromNavDescription,
        sendPlanToGeneralPractitioner = true,
        messageToGeneralPractitioner = "Message to GP",
        additionalInformation = "Additional information",
        contactPersonFullName = "Contact Person",
        contactPersonPhoneNumber = "12345678",
        contactPersonEmail = "contact@example.com",
        employeeHasContributedToPlan = employeeHasContributedToPlan,
        employeeHasNotContributedToPlanDescription = employeeHasNotContributedToPlanDescription,
        lpsName = "LPS Name",
        lpsEmail = "lps@lps.no",
    )
}

fun createValidAaregArbeidsforholdOversiktDTO(
    hovedEnhetOrgNr: String,
    underenhetOrgNr: String
): AaregArbeidsforholdOversikt {
    return AaregArbeidsforholdOversikt(
        listOf(
            Arbeidsforholdoversikt(
                arbeidssted = Arbeidssted(
                    type = ArbeidsstedType.Underenhet,
                    identer = listOf(
                        Ident(type = IdentType.ORGANISASJONSNUMMER, ident = underenhetOrgNr, gjeldende = true)
                    )
                ),
                opplysningspliktig = Opplysningspliktig(
                    type = OpplysningspliktigType.Hovedenhet,
                    identer = listOf(
                        Ident(type = IdentType.ORGANISASJONSNUMMER, ident = hovedEnhetOrgNr, gjeldende = true)
                    )
                )
            )
        )
    )
}

fun createValidSykmeldingsperioder(): List<Sykmeldingsperiode> {
    return listOf(
        Sykmeldingsperiode(
            uuid = UUID.randomUUID(),
            sykmeldingId = "sykmelding123",
            organizationNumber = UNDERENHET_ORGNUMBER,
            employeeIdentificationNumber = EMPLOYEE_SSN,
            fom = LocalDate.now().minusDays(10),
            tom = LocalDate.now().plusDays(10),
            createdAt = LocalDateTime.now()
        )
    )
}
