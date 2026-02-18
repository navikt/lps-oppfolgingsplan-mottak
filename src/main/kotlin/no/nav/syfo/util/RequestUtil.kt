package no.nav.syfo.util

import java.util.UUID

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
const val NAV_PERSONIDENT_HEADER = "nav-personident"
const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"

fun createCallId(): String = UUID.randomUUID().toString()

fun createBearerToken(token: String) = "Bearer $token"
