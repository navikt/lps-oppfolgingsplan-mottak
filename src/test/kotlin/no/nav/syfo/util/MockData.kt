package no.nav.syfo.util

import no.nav.syfo.client.krrproxy.domain.Kontaktinfo
import no.nav.syfo.client.krrproxy.domain.PostPersonerResponse

const val FNR_1 = "12345678901"
const val FNR_2 = "23456789012"

val dkifPostPersonerResponse =
    PostPersonerResponse(
        personer =
            mapOf(
                FNR_1 to Kontaktinfo(kanVarsles = true, reservert = false, mobiltelefonnummer = null, epostadresse = null),
                FNR_2 to Kontaktinfo(kanVarsles = false, reservert = true, mobiltelefonnummer = null, epostadresse = null),
            ),
    )

@Suppress("ConstructorParameterNaming")
val tokenFromAzureServer =
    Token(
        access_token = "AAD access token",
        token_type = "Bearer",
        expires_in = 3600,
    )

@Suppress("ConstructorParameterNaming")
data class Token(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
)
