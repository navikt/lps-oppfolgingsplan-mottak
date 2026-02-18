package no.nav.syfo.altinnmottak.kafka

import no.nav.syfo.altinnmottak.kafka.domain.KAltinnOppfolgingsplan
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.producerProperties
import no.nav.syfo.application.metric.COUNT_METRIKK_DELT_MED_NAV_FALSE
import no.nav.syfo.application.metric.COUNT_METRIKK_DELT_MED_NAV_TRUE
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.UUID

const val OPPFOLGINGSPLAN_LPS_NAV_TOPIC = "team-esyfo.aapen-syfo-oppfolgingsplan-lps-nav-v2"

class AltinnOppfolgingsplanProducer(
    env: KafkaEnv,
) {
    private val kafkaProducer: KafkaProducer<String, KAltinnOppfolgingsplan>
    private val log = LoggerFactory.getLogger(AltinnOppfolgingsplanProducer::class.qualifiedName)

    init {
        val kafkaConfig = producerProperties(env)
        kafkaProducer = KafkaProducer(kafkaConfig)
    }

    fun sendAltinnLpsToNav(kOppfolgingsplanLPS: KAltinnOppfolgingsplan) {
        try {
            val recordToSend =
                ProducerRecord(
                    OPPFOLGINGSPLAN_LPS_NAV_TOPIC,
                    UUID.randomUUID().toString(),
                    kOppfolgingsplanLPS,
                )
            kafkaProducer.send(recordToSend)
            COUNT_METRIKK_DELT_MED_NAV_TRUE.increment()
            log.info("Altinn-LPS sent to NAV")
        } catch (e: Exception) {
            COUNT_METRIKK_DELT_MED_NAV_FALSE.increment()
            log.error(
                "Encountered error while sending KOppfolgingsplanLps with UUID" +
                    "${kOppfolgingsplanLPS.uuid} to NAV",
            )
            throw RuntimeException("Could not send LPS to NAV", e)
        }
    }
}
