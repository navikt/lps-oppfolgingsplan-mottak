package no.nav.syfo.altinnmottak.kafka

import no.nav.syfo.altinnmottak.database.domain.AltinnLpsOppfolgingsplan
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

class KafkaAltinnLpsOppfolgingsplanDeserializer : Deserializer<AltinnLpsOppfolgingsplan?> {
    private val mapper = configuredJacksonMapper()

    override fun deserialize(topic: String, data: ByteArray): AltinnLpsOppfolgingsplan? =
        mapper.readValue(data, AltinnLpsOppfolgingsplan::class.java)

    override fun close() {}
}
