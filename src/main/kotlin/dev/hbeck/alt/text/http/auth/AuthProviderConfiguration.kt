package dev.hbeck.alt.text.http.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.oauth2.sdk.id.Audience
import com.nimbusds.oauth2.sdk.id.Issuer
import java.net.URI
import java.net.URL
import javax.validation.Valid


class AuthProviderConfiguration(
    @Valid @JsonProperty("jwkUrl") val jwkUrl: URL,
    @Valid @JsonProperty("algorithm") private val algorithmStr: String,
    @Valid @JsonProperty("audience") private val audienceUri: URI,
    @Valid @JsonProperty("issuer") private val issuerUri: URI,
) {
    fun getAlgorithm(): JWSAlgorithm {
        return JWSAlgorithm.parse(algorithmStr)
    }

    fun getAudience(): Audience {
        return Audience(audienceUri)
    }

    fun getIssuer(): Issuer {
        return Issuer(issuerUri)
    }
}