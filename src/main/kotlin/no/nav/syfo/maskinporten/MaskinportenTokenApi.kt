package no.nav.syfo.maskinporten

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.application.ApplicationEnvironment
import no.nav.syfo.client.httpClientDefault
import java.util.*

private const val TWO_MINUTES_IN_SECONDS = 120
private const val CONSUMER_ORG = "889640782"
private val httpClient = httpClientDefault()

fun Routing.registerMaskinportenTokenApi(
    env: ApplicationEnvironment
) {
    route("/api/test/token") {
        authenticate("test-token") {
            get {
                val log = call.application.log
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

                if (maskinportenResponse.access_token != null) {
                    call.respond(HttpStatusCode.OK, maskinportenResponse.access_token)
                } else {
                    val maskinportenError = maskinportenResponse.error
                    val maskinportenErrorDescription = maskinportenResponse.error_description

                    log.error("Token error: $maskinportenError Description: $maskinportenErrorDescription")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        errorMsg(maskinportenError, maskinportenErrorDescription)
                    )
                }
            }
        }
    }
}

private fun generateJwtGrant(env: ApplicationEnvironment): String {
    val authEnv = env.auth.maskinporten
    val rsaKey = RSAKey.parse(authEnv.clientJwk)
    val signedJwt = SignedJWT(
        rsaSignatureFromKey(rsaKey),
        jwtClaimSet(
            authEnv.scope,
            authEnv.issuer,
            authEnv.clientId,
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

private fun jwtClaimSet(
    scope: String,
    audience: String,
    clientId: String,
): JWTClaimsSet {
    val now = Date()
    return JWTClaimsSet.Builder()
        .audience(audience)
        .issuer(clientId)
        .claim("scope", scope)
        .claim("consumer_org", CONSUMER_ORG)
        .issueTime(now)
        .expirationTime(setExpirationTimeTwoMinutesAhead(now))
        .build()
}

private fun setExpirationTimeTwoMinutesAhead(issuedAt: Date): Date {
    val calendar = Calendar.getInstance()
    calendar.time = issuedAt
    calendar.add(Calendar.SECOND, TWO_MINUTES_IN_SECONDS)
    return calendar.time
}

private fun errorMsg(error: String?, description: String?) =
    "Got error while attempting to exchange JWT-grant for access token: $error --- $description"
