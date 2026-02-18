package no.nav.syfo.application.kafka

import io.ktor.server.application.Application
import kotlinx.coroutines.launch
import no.nav.syfo.altinnmottak.AltinnLpsService
import no.nav.syfo.altinnmottak.kafka.AltinnOppfolgingsplanConsumer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.sykmelding.SendtSykmeldingAivenConsumer
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import kotlin.coroutines.CoroutineContext

fun Application.kafkaModule(
    appState: ApplicationState,
    backgroundTasksContext: CoroutineContext,
    altinnLPSService: AltinnLpsService,
    sykmeldingService: SendtSykmeldingService,
    env: Environment,
) {
    launch(backgroundTasksContext) {
        launchKafkaListener(
            appState,
            AltinnOppfolgingsplanConsumer(env.kafka, altinnLPSService),
        )
    }

    launch(backgroundTasksContext) {
        launchKafkaListener(
            appState,
            SendtSykmeldingAivenConsumer(env.kafka, sykmeldingService),
        )
    }
}

suspend fun launchKafkaListener(
    applicationState: ApplicationState,
    kafkaListener: KafkaListener,
) {
    try {
        kafkaListener.listen(applicationState)
    } finally {
        applicationState.ready = false
    }
}
