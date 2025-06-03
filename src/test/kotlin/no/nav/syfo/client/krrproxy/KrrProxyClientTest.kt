package no.nav.syfo.client.krrproxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.util.MockServers
import no.nav.syfo.util.FNR_1
import no.nav.syfo.util.FNR_2

class KrrProxyClientTest : DescribeSpec({
    val localAppPropertiesPath = "./src/main/resources/localEnv.json"
    val fnrNonReservedUser = FNR_1
    val fnrReservedUser = FNR_2

    val objectMapper = ObjectMapper().registerKotlinModule()
    val testEnv = objectMapper.readValue(
        File(localAppPropertiesPath),
        ApplicationEnvironment::class.java
    )
    val auth = testEnv.auth.copy(
        azuread = testEnv.auth.azuread.copy(
            accessTokenUrl = "http://localhost:9595",
            wellKnownUrl = "http://localhost:9591"
        )
    )
    val mockServers = MockServers(testEnv.urls, auth)
    val azureAdMockServer = mockServers.mockAADServer()
    val krrMockServer = mockServers.mockKrrServer()

    val azureAdConsumer = AzureAdClient(auth)
    val dkifConsumer = KrrProxyClient(testEnv.urls, azureAdConsumer)

    beforeSpec {
        azureAdMockServer.start()
        krrMockServer.start()
    }

    afterSpec {
        azureAdMockServer.stop(1L, 10L)
        krrMockServer.stop(1L, 10L)
    }

    describe("KrrProxyClientTest") {
        it("Call KrrProxy for non-reserved user") {
            val dkifResponse = dkifConsumer.person(fnrNonReservedUser)
            dkifResponse shouldNotBe null
            dkifResponse!!.kanVarsles shouldBe true
        }

        it("Call KrrProxy for reserved user") {
            val dkifResponse = dkifConsumer.person(fnrReservedUser)
            dkifResponse shouldNotBe null
            dkifResponse!!.kanVarsles shouldBe false
        }

        it("function person should return null on when request causes exception") {
            val dkifResponse = dkifConsumer.person("serverdown")
            dkifResponse shouldBe null
        }
    }
})
