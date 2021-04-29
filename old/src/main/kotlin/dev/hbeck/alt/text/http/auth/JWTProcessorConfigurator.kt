package dev.hbeck.alt.text.http.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.ResourceRetriever
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import java.net.URL


class JWTProcessorConfigurator<T : SecurityContext>(
    private val jwkSourceUrl: URL,
    private val retriever: ResourceRetriever,
    private val algorithm: JWSAlgorithm
) {
    fun configureJWTProcessor(processor: DefaultJWTProcessor<T>) {
        val jwkSet = RemoteJWKSet<T>(jwkSourceUrl, retriever)
        processor.jwsKeySelector = JWSVerificationKeySelector(algorithm, jwkSet)
    }
}