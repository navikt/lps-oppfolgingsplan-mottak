package no.nav.syfo.client.azuread

import java.io.Serializable
import java.time.LocalDateTime

data class AzureAdToken(
    val accessToken: String?,
    val expires: LocalDateTime,
) : Serializable

private const val SECONDS_BEFORE_EXPIRY = 60L

fun AzureAdToken.isExpired() = this.expires < LocalDateTime.now().plusSeconds(SECONDS_BEFORE_EXPIRY)
