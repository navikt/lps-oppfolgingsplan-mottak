package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
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
import no.nav.syfo.application.kafka.kafkaModule
import no.nav.syfo.application.scheduling.schedulerModule
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

const val SERVER_SHUTDOWN_GRACE_PERIOD = 10L
const val SERVER_SHUTDOWN_TIMEOUT = 10L
const val THREAD_POOL_WORKER_GROUP_SIZE = 8
const val THREAD_POOL_CALL_GROUP_SIZE = 16
const val THREAD_POOL_CONNECTION_GROUP_SIZE = 8

lateinit var database: DatabaseInterface

fun main() {
    val server = embeddedServer(
        factory = Netty,
        environment = createApplicationEngineEnvironment(),
    ) {
        connectionGroupSize = THREAD_POOL_CONNECTION_GROUP_SIZE
        workerGroupSize = THREAD_POOL_WORKER_GROUP_SIZE
        callGroupSize = THREAD_POOL_CALL_GROUP_SIZE
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(SERVER_SHUTDOWN_GRACE_PERIOD, SERVER_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)
        },
    )

    server.start(wait = true)
}

@Suppress("LongMethod")
private fun createApplicationEngineEnvironment(): ApplicationEngineEnvironment {
    val logger = LoggerFactory.getLogger("ktor.application")
    val appState = ApplicationState()
    val appEnv = getEnv()
    val backgroundTasksContext = Executors.newFixedThreadPool(
        appEnv.application.coroutineThreadPoolSize,
    ).asCoroutineDispatcher()
    database = Database(appEnv.database)
    database.grantAccessToIAMUsers()
    val azureAdClient = AzureAdClient(appEnv.auth)
    val isdialogmeldingClient = IsdialogmeldingClient(appEnv.urls, azureAdClient)
    val pdlClient = PdlClient(appEnv.urls, azureAdClient)
    val krrProxyClient = KrrProxyClient(appEnv.urls, azureAdClient)
    val eregClient = EregClient(appEnv.urls, appEnv.application, azureAdClient)
    val pdfGenClient = OpPdfGenClient(appEnv.urls, appEnv.application, pdlClient, krrProxyClient)
    val navLpsProducer = AltinnOppfolgingsplanProducer(appEnv.kafka)
    val dokarkivClient = DokarkivClient(appEnv.urls, azureAdClient, eregClient)

    val altinnLpsService = AltinnLpsService(
        pdlClient,
        pdfGenClient,
        database,
        navLpsProducer,
        isdialogmeldingClient,
        dokarkivClient,
        appEnv.altinnLps.sendToFastlegeRetryThreshold,
        appEnv.toggles,
    )

    val sykmeldingService = SendtSykmeldingService(database)

    val followupPlanProducer = FollowUpPlanProducer(appEnv.kafka)

    val followUpPlanSendingService = FollowUpPlanSendingService(
        isdialogmeldingClient,
        followupPlanProducer,
        pdfGenClient,
        dokarkivClient,
        appEnv.toggles
    )

    val wellKnownInternalAzureAD = getWellKnown(
        wellKnownUrl = appEnv.auth.azuread.wellKnownUrl,
    )

    val wellKnownMaskinporten = getWellKnown(
        wellKnownUrl = appEnv.auth.maskinporten.wellKnownUrl,
    )

    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        url = appEnv.urls.istilgangskontrollUrl,
        clientId = appEnv.urls.istilgangskontrollClientId,
    )

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())
        connector {
            port = appEnv.application.port
        }
        module {
            apiModule(
                appState,
                database,
                appEnv,
                wellKnownMaskinporten,
                wellKnownInternalAzureAD,
                veilederTilgangskontrollClient,
                followUpPlanSendingService,
                pdlClient,
                sykmeldingService
            )
            kafkaModule(
                appState,
                backgroundTasksContext,
                altinnLpsService,
                sykmeldingService,
                appEnv,
            )
            schedulerModule(
                backgroundTasksContext,
                database,
                altinnLpsService,
                appEnv,
            )
        }
    }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        appState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
    }

    return applicationEngineEnvironment
}
