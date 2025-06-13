package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.asCoroutineDispatcher
import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.altinnmottak.kafka.AltinnOppfolgingsplanProducer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.Database
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.grantAccessToIAMUsers
import no.nav.syfo.application.environment.getEnv
import no.nav.syfo.application.environment.isDev
import no.nav.syfo.application.kafka.kafkaModule
import no.nav.syfo.application.scheduling.schedulerModule
import no.nav.syfo.client.aareg.ArbeidsforholdOversiktClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.dokarkiv.DokarkivClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.isdialogmelding.IsdialogmeldingClient
import no.nav.syfo.client.krrproxy.KrrProxyClient
import no.nav.syfo.client.oppdfgen.OpPdfGenClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.client.wellknown.getWellKnown
import no.nav.syfo.oppfolgingsplanmottak.kafka.FollowUpPlanProducer
import no.nav.syfo.oppfolgingsplanmottak.service.FollowUpPlanSendingService
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import no.nav.syfo.application.Environment

const val SERVER_SHUTDOWN_GRACE_PERIOD = 10L
const val SERVER_SHUTDOWN_TIMEOUT = 10L
const val THREAD_POOL_WORKER_GROUP_SIZE = 8
const val THREAD_POOL_CALL_GROUP_SIZE = 16
const val THREAD_POOL_CONNECTION_GROUP_SIZE = 8

lateinit var database: DatabaseInterface

fun main() {
    val logger = LoggerFactory.getLogger("ktor.application")
    val env = getEnv()
    val appState = ApplicationState()
    val server = embeddedServer(
        factory = Netty,
        environment = createApplicationEnvironment(env),
        configure = {
            connector {
                port = env.application.port
            }
            connectionGroupSize = THREAD_POOL_CONNECTION_GROUP_SIZE
            workerGroupSize = THREAD_POOL_WORKER_GROUP_SIZE
            callGroupSize = THREAD_POOL_CALL_GROUP_SIZE
        },
        module = setModule(env, appState)
    )
    server.monitor.subscribe(ApplicationStarted) { application ->
        appState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
    }


    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(SERVER_SHUTDOWN_GRACE_PERIOD, SERVER_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)
        },
    )

    server.start(wait = true)
}

fun createApplicationEnvironment(env: Environment): ApplicationEnvironment = applicationEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load())
    database = Database(env.database)
    database.grantAccessToIAMUsers()
}

@Suppress("LongMethod")
private fun setModule(env: Environment, appState: ApplicationState): _root_ide_package_.io.ktor.server.application.Application.() -> Unit = {
    val backgroundTasksContext = Executors.newFixedThreadPool(
        env.application.coroutineThreadPoolSize,
    ).asCoroutineDispatcher()
    database = Database(env.database)
    database.grantAccessToIAMUsers()
    val azureAdClient = AzureAdClient(env.auth)
    val isdialogmeldingClient = IsdialogmeldingClient(env.urls, azureAdClient)
    val pdlClient = PdlClient(env.urls, azureAdClient)
    val krrProxyClient = KrrProxyClient(env.urls, azureAdClient)
    val eregClient = EregClient(env.urls, env.application, azureAdClient)
    val pdfGenClient = OpPdfGenClient(env.urls, env.application, pdlClient, krrProxyClient)
    val navLpsProducer = AltinnOppfolgingsplanProducer(env.kafka)
    val dokarkivClient = DokarkivClient(env.urls, azureAdClient, eregClient)
    val arbeidsforholdOversiktClient = ArbeidsforholdOversiktClient(azureAdClient, env.urls)

    val altinnLpsService = AltinnLpsService(
        pdlClient,
        pdfGenClient,
        database,
        navLpsProducer,
        isdialogmeldingClient,
        dokarkivClient,
        env.altinnLps.sendToFastlegeRetryThreshold,
        env.toggles,
    )

    val sykmeldingService = SendtSykmeldingService(database)

    val followupPlanProducer = FollowUpPlanProducer(env.kafka)

    val followUpPlanSendingService = FollowUpPlanSendingService(
        isdialogmeldingClient,
        followupPlanProducer,
        pdfGenClient,
        dokarkivClient,
        env.isDev(),
    )

    val wellKnownInternalAzureAD = getWellKnown(
        wellKnownUrl = env.auth.azuread.wellKnownUrl,
    )

    val wellKnownMaskinporten = getWellKnown(
        wellKnownUrl = env.auth.maskinporten.wellKnownUrl,
    )

    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        url = env.urls.istilgangskontrollUrl,
        clientId = env.urls.istilgangskontrollClientId,
    )

    apiModule(
        appState,
        database,
        env,
        wellKnownMaskinporten,
        wellKnownInternalAzureAD,
        veilederTilgangskontrollClient,
        followUpPlanSendingService,
        pdlClient,
        sykmeldingService,
        arbeidsforholdOversiktClient
    )
    kafkaModule(
        appState,
        backgroundTasksContext,
        altinnLpsService,
        sykmeldingService,
        env,
    )
    schedulerModule(
        backgroundTasksContext,
        database,
        altinnLpsService,
        env,
    )
}
