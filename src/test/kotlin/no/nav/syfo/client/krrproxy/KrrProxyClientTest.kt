package no.nav.syfo.client.krrproxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.time.LocalDateTime
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.azuread.AzureAdToken
import no.nav.syfo.util.FNR_1
import no.nav.syfo.util.FNR_2
import no.nav.syfo.util.MockServers

class KrrProxyClientTest : DescribeSpec({
    val localAppPropertiesPath = "./src/main/resources/localEnv.json"
    val fnrNonReservedUser = FNR_1
    val fnrReservedUser = FNR_2

    val objectMapper = ObjectMapper().registerKotlinModule()
    val testEnv = objectMapper.readValue(
        File(localAppPropertiesPath),
        ApplicationEnvironment::class.java
    )

    val azureAdConsumer = mockk<AzureAdClient>()
    val mockServers = MockServers(testEnv.urls, testEnv.auth)
    val krrMockServer = mockServers.mockKrrServer()
    val dkifConsumer = KrrProxyClient(testEnv.urls, azureAdConsumer)

    beforeSpec {
        clearAllMocks()
        coEvery { azureAdConsumer.getSystemToken(any()) } returns AzureAdToken(
            accessToken = "AAD access token",
            expires = LocalDateTime.now().plusMinutes(5),
        )
        krrMockServer.start()
    }

    afterSpec {
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
