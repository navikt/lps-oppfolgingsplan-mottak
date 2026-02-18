package no.nav.syfo.mockdata

import io.ktor.server.application.Application
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.aareg.ArbeidsforholdOversiktClient
import no.nav.syfo.client.aareg.domain.AaregArbeidsforholdOversikt
import no.nav.syfo.client.aareg.domain.Arbeidsforholdoversikt
import no.nav.syfo.client.aareg.domain.Arbeidssted
import no.nav.syfo.client.aareg.domain.ArbeidsstedType
import no.nav.syfo.client.aareg.domain.Ident
import no.nav.syfo.client.aareg.domain.IdentType
import no.nav.syfo.client.aareg.domain.Opplysningspliktig
import no.nav.syfo.client.aareg.domain.OpplysningspliktigType
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.krrproxy.KrrProxyClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.mockdata.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.mockdata.UserConstants.ARBEIDSTAKER_FNR_NO_ARBEIDSFORHOLD
import no.nav.syfo.mockdata.UserConstants.HOVEDENHETSNUMMER
import no.nav.syfo.mockdata.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.sykmelding.service.SendtSykmeldingService

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    database: DatabaseInterface,
) {
    val isdialogmeldingClient = mockk<IsdialogmeldingClient>(relaxed = true)
    val followupPlanProducer = mockk<FollowUpPlanProducer>(relaxed = true)
    val dokarkivClient = mockk<DokarkivClient>(relaxed = true)

    val opPdfGenClient =
        OpPdfGenClient(
            externalMockEnvironment.environment.urls,
            externalMockEnvironment.environment.application,
            PdlClient(
                externalMockEnvironment.environment.urls,
                externalMockEnvironment.azureAdClient,
                externalMockEnvironment.mockHttpClient,
            ),
            KrrProxyClient(
                externalMockEnvironment.environment.urls,
                externalMockEnvironment.azureAdClient,
                client = externalMockEnvironment.mockHttpClient,
            ),
            externalMockEnvironment.mockHttpClient,
        )

    val veilederTilgangskontrollClient =
        VeilederTilgangskontrollClient(
            azureAdClient = externalMockEnvironment.azureAdClient,
            httpClient = externalMockEnvironment.mockHttpClient,
            url = externalMockEnvironment.environment.urls.istilgangskontrollUrl,
            clientId = externalMockEnvironment.environment.urls.istilgangskontrollClientId,
        )

    val followUpPlanSendingService =
        FollowUpPlanSendingService(
            isdialogmeldingClient = isdialogmeldingClient,
            followupPlanProducer = followupPlanProducer,
            opPdfGenClient = opPdfGenClient,
            dokarkivClient = dokarkivClient,
            isDev = false,
        )

    val sykmeldingService = SendtSykmeldingService(database)

    val arbeidsforholdOversiktClient = mockk<ArbeidsforholdOversiktClient>(relaxed = true)
    coEvery { arbeidsforholdOversiktClient.getArbeidsforhold(ARBEIDSTAKER_FNR_NO_ARBEIDSFORHOLD) } returns null
    coEvery { arbeidsforholdOversiktClient.getArbeidsforhold(ARBEIDSTAKER_FNR) } returns
        AaregArbeidsforholdOversikt(
            arbeidsforholdoversikter =
                listOf(
                    Arbeidsforholdoversikt(
                        arbeidssted =
                            Arbeidssted(
                                type = ArbeidsstedType.Underenhet,
                                identer =
                                    listOf(
                                        Ident(
                                            type = IdentType.ORGANISASJONSNUMMER,
                                            ident = VIRKSOMHETSNUMMER,
                                            gjeldende = true,
                                        ),
                                    ),
                            ),
                        opplysningspliktig =
                            Opplysningspliktig(
                                type = OpplysningspliktigType.Hovedenhet,
                                identer =
                                    listOf(
                                        Ident(
                                            type = IdentType.ORGANISASJONSNUMMER,
                                            ident = HOVEDENHETSNUMMER,
                                            gjeldende = true,
                                        ),
                                    ),
                            ),
                    ),
                    Arbeidsforholdoversikt(
                        arbeidssted =
                            Arbeidssted(
                                type = ArbeidsstedType.Person,
                                identer =
                                    listOf(
                                        Ident(
                                            type = IdentType.FOLKEREGISTERIDENT,
                                            ident = "213",
                                            gjeldende = false,
                                        ),
                                    ),
                            ),
                        opplysningspliktig =
                            Opplysningspliktig(
                                type = OpplysningspliktigType.Person,
                                identer =
                                    listOf(
                                        Ident(
                                            type = IdentType.FOLKEREGISTERIDENT,
                                            ident = "325",
                                            gjeldende = false,
                                        ),
                                    ),
                            ),
                    ),
                ),
        )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = database,
        environment = externalMockEnvironment.environment,
        wellKnownMaskinporten = externalMockEnvironment.wellKnownMaskinporten,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        followUpPlanSendingService = followUpPlanSendingService,
        pdlClient =
            PdlClient(
                externalMockEnvironment.environment.urls,
                externalMockEnvironment.azureAdClient,
                externalMockEnvironment.mockHttpClient,
            ),
        sykmeldingService = sykmeldingService,
        arbeidsforholdOversiktClient = arbeidsforholdOversiktClient,
    )
}
