package no.nav.syfo.application.kafka

import io.ktor.server.application.*
import kotlinx.coroutines.launch
import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.altinnmottak.kafka.LpsMigrationKafkaConsumer
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.database
import kotlin.coroutines.CoroutineContext

fun Application.kafkaModule(
    appState: ApplicationState,
    backgroundTasksContext: CoroutineContext,
    altinnLPSService: AltinnLpsService,
    env: ApplicationEnvironment,
) {
//    launch(backgroundTasksContext) {
//        try {
//            val lpsOppfolgingsplanKafkaConsumer = AltinnOppfolgingsplanConsumer(env.kafka, altinnLPSService)
//            lpsOppfolgingsplanKafkaConsumer.listen(appState)
//        } finally {
//            appState.ready = false
//        }
//    }
    launch(backgroundTasksContext) {
        try {
            val lpsMigrationKafkaConsumer = LpsMigrationKafkaConsumer(env.kafka, database)
            lpsMigrationKafkaConsumer.listen(appState)
        } finally {
            appState.ready = false
        }
    }
}
