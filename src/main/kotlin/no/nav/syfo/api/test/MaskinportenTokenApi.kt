package no.nav.syfo.api.test

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.*
import no.nav.syfo.api.util.httpClient
import no.nav.syfo.environment.Environment
import org.slf4j.LoggerFactory
import java.util.*

private const val twoMinutesInSeconds = 120
private val httpClient = httpClient()

fun Routing.registerMaskinportenTokenApi(
    env: Environment
) {
    route("/api/test/token") {
        get {
            val jwtGrant = generateJwtGrant(env)
            val maskinportenTokenUrl = env.auth.maskinporten.tokenUrl
            val response: HttpResponse = httpClient.submitForm(
                url = maskinportenTokenUrl,
                formParameters = parameters {
                    append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    append("assertion", jwtGrant)
                }
            )
            val maskinportenResponse: MaskinportenResponse = response.body()
            val maskinPortenAccessToken = maskinportenResponse.access_token
            maskinPortenAccessToken?.let { token ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = token
                )
            }
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = errorMsg(maskinportenResponse.error, maskinportenResponse.error_description)
            )
        }
    }
}

private fun generateJwtGrant(env: Environment): String {
    val authEnv = env.auth.maskinporten
    val scope = authEnv.scope
    val audience = authEnv.issuer
    val clientId = authEnv.clientId
    val clientJwk = authEnv.clientJwk
    val rsaKey = RSAKey.parse(clientJwk)
    val signedJwt = SignedJWT(
        rsaSignatureFromKey(rsaKey),
        jwtClaimSet(
            scope,
            audience,
            clientId
        )
    )
    signedJwt.sign(RSASSASigner(rsaKey.toPrivateKey()))
    return signedJwt.serialize()
}

private fun rsaSignatureFromKey(key: RSAKey) =
    JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(key.keyID)
        .type(JOSEObjectType.JWT)
        .build()

@OptIn(InternalAPI::class)
private fun jwtClaimSet(
    scope: String,
    audience: String,
    clientId: String
): JWTClaimsSet {
    val now = Date()
    return JWTClaimsSet.Builder()
        .audience(audience)
        .issuer(clientId)
        .claim("scope", scope)
        .issueTime(now)
        .expirationTime(setExpirationTimeTwoMinutesAhead(now))
        .build()

}
private fun setExpirationTimeTwoMinutesAhead(issuedAt: Date): Date {
    val calendar = Calendar.getInstance()
    calendar.time = issuedAt
    calendar.add(Calendar.SECOND, twoMinutesInSeconds)
    return calendar.time
}

private fun errorMsg(error: String?, description: String?) =
    "Got error while attempting to exchange JWT-grant for access token: $error --- $description"