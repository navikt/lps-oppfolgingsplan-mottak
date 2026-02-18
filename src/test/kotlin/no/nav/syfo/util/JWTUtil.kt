package no.nav.syfo.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import no.nav.syfo.mockdata.ExternalMockEnvironment
import no.nav.syfo.mockdata.UserConstants
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

fun validVeilederToken(navIdent: String = UserConstants.VEILEDER_IDENT) =
    generateAzureAdJWT(
        audience = ExternalMockEnvironment.instance.environment.auth.azuread.clientId,
        issuer = ExternalMockEnvironment.instance.wellKnownInternalAzureAD.issuer,
        navIdent = navIdent,
    )

fun validMaskinportenToken(
    consumerOrgnumber: String = "123456789",
    supplierOrgnumber: String = "889640782",
) = generateMaskinportenJWT(
    issuer = ExternalMockEnvironment.instance.wellKnownMaskinporten.issuer,
    scope = ExternalMockEnvironment.instance.environment.auth.maskinporten.scope,
    consumerOrgnumber = consumerOrgnumber,
    supplierOrgnumber = supplierOrgnumber,
    expiry = LocalDateTime.now().plusHours(1),
)

fun customMaskinportenToken(
    consumerOrgnumber: String? = "123456789",
    supplierOrgnumber: String? = "889640782",
    scope: String = ExternalMockEnvironment.instance.environment.auth.maskinporten.scope,
    expiry: LocalDateTime = LocalDateTime.now().plusHours(1),
) = generateMaskinportenJWT(
    issuer = ExternalMockEnvironment.instance.wellKnownMaskinporten.issuer,
    scope = scope,
    consumerOrgnumber = consumerOrgnumber,
    supplierOrgnumber = supplierOrgnumber,
    expiry = expiry,
)

const val KEY_ID = "localhost-signer"

// Mock of JWT-token supplied by AzureAD. KeyId must match kid in jwkset.json
fun generateAzureAdJWT(
    audience: String,
    issuer: String,
    navIdent: String? = null,
    expiry: LocalDateTime? = LocalDateTime.now().plusHours(1),
): String {
    val now = Date()
    val key = getDefaultRSAKey()
    val alg = Algorithm.RSA256(key.toRSAPublicKey(), key.toRSAPrivateKey())

    return JWT
        .create()
        .withKeyId(KEY_ID)
        .withIssuer(issuer)
        .withAudience(audience)
        .withJWTId(UUID.randomUUID().toString())
        .withClaim("iat", now)
        .withClaim("exp", Date.from(expiry?.atZone(ZoneId.systemDefault())?.toInstant()))
        .withClaim(JWT_CLAIM_NAVIDENT, navIdent)
        .sign(alg)
}

// Mock of JWT-token supplied by AzureAD. KeyId must match kid in jwkset.json
fun generateMaskinportenJWT(
    issuer: String,
    scope: String,
    expiry: LocalDateTime?,
    consumerOrgnumber: String?,
    supplierOrgnumber: String?,
): String {
    val now = Date()
    val key = getDefaultRSAKey()
    val alg = Algorithm.RSA256(key.toRSAPublicKey(), key.toRSAPrivateKey())

    val jwtBuilder =
        JWT
            .create()
            .withKeyId(KEY_ID)
            .withIssuer(issuer)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("iat", now)
            .withClaim("scope", scope)
            .withExpiresAt(expiry?.let { Date.from(it.atZone(ZoneId.systemDefault()).toInstant()) })

    if (!consumerOrgnumber.isNullOrEmpty()) {
        jwtBuilder.withClaim(
            "consumer",
            mapOf("authority" to "iso6523-actorid-upis", "ID" to "0192:$consumerOrgnumber"),
        )
    }

    if (!supplierOrgnumber.isNullOrEmpty()) {
        jwtBuilder.withClaim(
            "supplier",
            mapOf("authority" to "iso6523-actorid-upis", "ID" to "0192:$supplierOrgnumber"),
        )
    }

    return jwtBuilder.sign(alg)
}

private fun getDefaultRSAKey(): RSAKey = getJWKSet().getKeyByKeyId(KEY_ID) as RSAKey

private fun getJWKSet(): JWKSet {
    val jwkSet = getFileAsString("src/test/resources/jwkset.json")
    try {
        return JWKSet.parse(jwkSet)
    } catch (io: IOException) {
        throw RuntimeException(io)
    } catch (io: ParseException) {
        throw RuntimeException(io)
    }
}

fun getFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
