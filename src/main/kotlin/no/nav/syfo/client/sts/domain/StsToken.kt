package no.nav.syfo.client.sts.domain

import java.time.LocalDateTime

data class StsToken(
        val access_token: String,
        val token_type: String,
        val expires_in: Int
) {
    // Expire 10 seconds before token expiration

    val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expires_in - 10L)

    companion object {
        fun shouldRenew(token: StsToken?): Boolean {
            if (token == null) {
                return true
            }

            return isExpired(token)
        }

        private fun isExpired(token: StsToken): Boolean {
            return token.expirationTime.isBefore(LocalDateTime.now())
        }
    }
}