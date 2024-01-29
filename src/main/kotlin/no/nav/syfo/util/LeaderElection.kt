package no.nav.syfo.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.application.environment.ApplicationEnv
import java.net.InetAddress

class LeaderElection(val application: ApplicationEnv) {
    val objectMapper = ObjectMapper().registerKotlinModule()

    fun thisPodIsLeader(): Boolean {
        val electorPath = application.electorPath
        val electorPollUrl = "http://$electorPath"
        return runBlocking {
            val leaderPod = getLeaderPod(electorPollUrl)
            val podHostname: String = withContext(Dispatchers.IO) {
                InetAddress.getLocalHost()
            }.hostName
            podHostname == leaderPod
        }
    }

    private suspend fun getLeaderPod(path: String): String {
        val leaderJsonString = callElectorPath(path)
        return parseLeaderJson(leaderJsonString)
    }

    private suspend fun callElectorPath(path: String): String {
        val client = httpClientDefault()
        val leaderResponse = client.get(path) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json)
            }
        }
        return leaderResponse.body()
    }

    private fun parseLeaderJson(leaderJsonString: String): String {
        val leaderJson = objectMapper.readTree(leaderJsonString)
        return leaderJson["name"].toString().replace("\"", "")
    }
}
