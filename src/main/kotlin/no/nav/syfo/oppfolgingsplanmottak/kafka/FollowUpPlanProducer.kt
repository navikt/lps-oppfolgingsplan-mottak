package no.nav.syfo.oppfolgingsplanmottak.kafka

import java.util.*
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.producerProperties
import no.nav.syfo.application.metric.COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_NAV_FALSE
import no.nav.syfo.application.metric.COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_NAV_TRUE
import no.nav.syfo.oppfolgingsplanmottak.kafka.domain.KFollowUpPlan
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class FollowUpPlanProducer(
    env: KafkaEnv,
) {
    private val kafkaProducer: KafkaProducer<String, KFollowUpPlan>
    private val log = LoggerFactory.getLogger(FollowUpPlanProducer::class.qualifiedName)

    init {
        val kafkaConfig = producerProperties(env)
        kafkaProducer = KafkaProducer(kafkaConfig)
    }

    fun createFollowUpPlanTaskInModia(kFollowupPlan: KFollowUpPlan) {
        try {
            val recordToSend = ProducerRecord(
                "team-esyfo.aapen-syfo-oppfolgingsplan-lps-nav-v2",
                UUID.randomUUID().toString(),
                kFollowupPlan
            )
            kafkaProducer.send(recordToSend)
            log.info("Followup-plan task sent to Modia with UUID ${kFollowupPlan.uuid}")
            COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_NAV_TRUE.increment()
        } catch (e: Exception) {
            COUNT_METRIKK_FOLLOWUP_LPS_DELT_MED_NAV_FALSE.increment()
            log.error(
                "Encountered error while sending KFollowupPlan with UUID ${kFollowupPlan.uuid} to Modia"
            )
            throw RuntimeException("Could not send followup-plan task to Modia", e)
        }
    }
}
