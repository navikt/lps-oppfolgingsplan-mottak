package no.nav.syfo.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.op2016.Oppfoelgingsplan4UtfyllendeInfoM

const val AR_1 = "AR0000000"
const val AR_2 = "AR0000001"

val xmlMapper: ObjectMapper = XmlMapper(
    JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    },
).registerModule(JaxbAnnotationModule())
    .registerKotlinModule()
    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

class LpsHelper {
    fun receiveLps(): Triple<String, String, String> {
        val (fnr, payload) = this.loadXML("/lps/lps_test.xml")
        return Triple(AR_1, fnr, payload)
    }

    fun receiveLpsWithoutDelingSet(): Triple<String, String, String> {
        val (fnr, payload) = loadXML("/lps/lps_test_ingen_deling.xml")
        return Triple(AR_2, fnr, payload)
    }

    private fun loadXML(resourcePath: String): Pair<String, String> {
        val payload = this::class.java.getResource(resourcePath).readText()
        val fnr = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(payload).skjemainnhold.sykmeldtArbeidstaker.fnr
        return Pair(fnr, payload)
    }
}
fun String.extractPortFromUrl(): Int {
    val portIndexStart = lastIndexOf(':') + 1
    val urlLastPortion = subSequence(portIndexStart, length)
    var portIndexEnd = urlLastPortion.indexOf('/')
    if (portIndexEnd == -1) {
        portIndexEnd = portIndexStart + urlLastPortion.length
    } else {
        portIndexEnd += portIndexStart
    }
    return subSequence(portIndexStart, portIndexEnd).toString().toInt()
}
