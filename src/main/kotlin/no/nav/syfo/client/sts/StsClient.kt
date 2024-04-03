package no.nav.syfo.client.sts

import no.nav.syfo.client.sts.domain.StsToken
import org.slf4j.LoggerFactory

class StsClient {
    private var cachedOidcToken: StsToken? = null

    fun token(): String {
        return "todo"
//        if (StsToken.shouldRenew(cachedOidcToken)) {
//            val request = HttpEntity<Any>(authorizationHeader())
//            try {
//                val response = restTemplate.exchange(
//                    stsTokenUrl,
//                    HttpMethod.GET,
//                    request,
//                    StsToken::class.java
//                )
//                cachedOidcToken = response.body
//            } catch (e: RestClientResponseException) {
//                LOG.error("Request to get STS failed with status: ${e.statusCode.value()} and message: ${e.responseBodyAsString}")
//                throw e
//            }
//        }
//        return cachedOidcToken!!.access_token
    }

//    private val stsTokenUrl: String
//        get() = "$url/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
//
//    private fun authorizationHeader(): HttpHeaders {
//        val credentials = basicCredentials(username, password)
//        val headers = HttpHeaders()
//        headers.add(HttpHeaders.AUTHORIZATION, credentials)
//        return headers
//    }
//
//    companion object {
//        private val LOG = LoggerFactory.getLogger(StsConsumer::class.java)
//
//        const val METRIC_CALL_STS_SUCCESS = "call_sts_success"
//        const val METRIC_CALL_STS_FAIL = "call_sts_fail"
//    }
}
