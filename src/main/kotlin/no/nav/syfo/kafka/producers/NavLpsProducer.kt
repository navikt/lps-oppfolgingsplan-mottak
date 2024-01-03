package no.nav.syfo.kafka.producers

import no.nav.syfo.environment.KafkaEnv
import no.nav.syfo.kafka.KOppfolgingsplanLps
import no.nav.syfo.kafka.OPPFOLGINGSPLAN_LPS_NAV_TOPIC
import no.nav.syfo.kafka.producerProperties
import no.nav.syfo.metrics.COUNT_METRIKK_DELT_MED_NAV_FALSE
import no.nav.syfo.metrics.COUNT_METRIKK_DELT_MED_NAV_TRUE
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

class NavLpsProducer(
   val env: KafkaEnv
) {
    private val kafkaProducer: KafkaProducer<String, KOppfolgingsplanLps>
    private val log = LoggerFactory.getLogger(NavLpsProducer::class.qualifiedName)

    init {
        val kafkaConfig = producerProperties(env)
        kafkaProducer = KafkaProducer(kafkaConfig)
    }

    fun sendAltinnLpsToNav(kOppfolgingsplanLPS: KOppfolgingsplanLps) {
        try {
            val recordToSend = ProducerRecord(
                OPPFOLGINGSPLAN_LPS_NAV_TOPIC,
                UUID.randomUUID().toString(),
                kOppfolgingsplanLPS
            )
            kafkaProducer.send(recordToSend)
            COUNT_METRIKK_DELT_MED_NAV_TRUE.increment()
            log.info("Altinn-LPS sent to NAV")
        } catch (e: Exception) {
            COUNT_METRIKK_DELT_MED_NAV_FALSE.increment()
            log.error("Encountered error while sending KOppfolgingsplanLps with UUID" +
                    "${kOppfolgingsplanLPS.uuid} to NAV")
            throw RuntimeException("Could not send LPS to NAV", e)
        }
    }
}
