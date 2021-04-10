package dev.hbeck.alt.text.http.auth

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.oauth2.sdk.id.Audience
import com.nimbusds.oauth2.sdk.id.Issuer
import java.text.ParseException
import java.util.stream.Collectors
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response


class CredentialVerifier(
    private val expectedAudience: Audience,
    private val expectedIssuer: Issuer,
    private val acceptableDeltaMillis: Long,
    processorConfigurator: JWTProcessorConfigurator<SecurityContext>
) {
    private val jwtProcessor = DefaultJWTProcessor<SecurityContext>()

    init {
        processorConfigurator.configureJWTProcessor(jwtProcessor)
    }

    fun verify(credentials: String): JWTClaimsSet {
        val jwtClaimsSet = try {
            jwtProcessor.process(credentials, null)
        } catch (e: Exception) {
            when (e) {
                is BadJOSEException, is JOSEException, is ParseException -> {
                    throw WebApplicationException(Response.status(401, "Unauthorized").build())
                }
                else -> throw e
            }
        }

        verifyAudience(jwtClaimsSet)
        verifyIssuer(jwtClaimsSet)
        verifyExpiry(jwtClaimsSet)
        verifyIssuedAt(jwtClaimsSet)
        verifySubject(jwtClaimsSet)

        return jwtClaimsSet
    }

    private fun verifySubject(jwtClaimsSet: JWTClaimsSet) {
        val sub = jwtClaimsSet.subject ?: throw TokenVerificationException("Token has no subject field")
        if (sub.isBlank()) {
            throw TokenVerificationException("Token subject is blank")
        }
    }

    private fun verifyIssuedAt(jwtClaimsSet: JWTClaimsSet) {
        val iat = jwtClaimsSet.issueTime?.time ?: throw TokenVerificationException("Token had no issue time field")

        val now = System.currentTimeMillis()
        if (iat > now + acceptableDeltaMillis) {
            throw TokenVerificationException("Token was issued ${iat - now}ms in the future")
        }
    }

    private fun verifyExpiry(jwtClaimsSet: JWTClaimsSet) {
        val exp = jwtClaimsSet.expirationTime?.time ?: throw TokenVerificationException("Token has no expiration field")
        val now = System.currentTimeMillis()
        if (exp <= now) {
            throw TokenVerificationException("Token is ${now - exp}ms expired")
        }
    }

    private fun verifyIssuer(jwtClaimsSet: JWTClaimsSet) {
        val issuer = Issuer(jwtClaimsSet.issuer)
        if (!issuer.isValid) {
            throw TokenVerificationException("Invalid issuer: $jwtClaimsSet")
        }

        if (issuer != expectedIssuer) {
            throw TokenVerificationException("Wrong issuer. Expected '${expectedIssuer.value}' but was '$jwtClaimsSet'")
        }
    }

    private fun verifyAudience(jwtClaimsSet: JWTClaimsSet) {
        val audience = jwtClaimsSet.audience ?: throw TokenVerificationException("Token has no audience field")
        if (audience.size != 1) {
            val audienceStr = audience.stream().collect(Collectors.joining(","))
            throw TokenVerificationException("Expected single audience value, but got [$audienceStr]")
        }

        if (expectedAudience != Audience(audience[0])) {
            throw TokenVerificationException("Expected audience '${expectedAudience.value}' but got '${audience[0]}'")
        }
    }
}