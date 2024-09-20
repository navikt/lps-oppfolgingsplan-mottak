package no.nav.syfo.application.scheduling

import io.ktor.server.application.Application
import kotlinx.coroutines.launch
import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.altinnmottak.scheduling.AltinnLpsScheduler
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.util.LeaderElection
import kotlin.coroutines.CoroutineContext

fun Application.schedulerModule(
    backgroundTasksContext: CoroutineContext,
    database: DatabaseInterface,
    altinnLpsService: AltinnLpsService,
    env: ApplicationEnvironment,
) {
    val leaderElection = LeaderElection(env.application)

    launch(backgroundTasksContext) {
        val scheduler = AltinnLpsScheduler(
            database,
            altinnLpsService,
            leaderElection,
            env.toggles,
        ).startScheduler()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                scheduler.shutdown()
            }
        )
    }
}
