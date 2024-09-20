package no.nav.syfo.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.db.TestDB
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.testApiModule

data class TestApplicationSetup(
    val testDB: DatabaseInterface,
    val httpClient: HttpClient,
)

fun ApplicationTestBuilder.configureTestApplication(): TestApplicationSetup {
    val testDb = TestDB.database
    TestDB.clearAllData()

    application {
        testApiModule(ExternalMockEnvironment.instance, testDb)
    }

    val client =
        createClient {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

    return TestApplicationSetup(testDb, client)
}
