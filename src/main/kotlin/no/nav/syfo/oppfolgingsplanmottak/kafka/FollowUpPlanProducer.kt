package no.nav.syfo.oppfolgingsplanmottak.kafka

import java.util.*
import no.nav.syfo.altinnmottak.kafka.domain.KFollowUpPlan
import no.nav.syfo.application.environment.KafkaEnv
import no.nav.syfo.application.kafka.producerProperties
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

    fun sendFollowUpPlanToNav(kFollowupPlan: KFollowUpPlan) {
        try {
            val recordToSend = ProducerRecord(
                "team-esyfo.aapen-syfo-oppfolgingsplan-lps-nav-v2",
                UUID.randomUUID().toString(),
                kFollowupPlan
            )
            kafkaProducer.send(recordToSend)
            log.info("Followup-LPS sent to NAV")
        } catch (e: Exception) {
            log.error(
                "Encountered error while sending KFollowupPlan with UUID ${kFollowupPlan.uuid} to NAV"
            )
            throw RuntimeException("Could not send LPS to NAV", e)
        }
    }
}
