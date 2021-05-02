package dev.hbeck.alt.text.heuristics

import com.fasterxml.jackson.annotation.JsonProperty
import io.pinecone.PineconeClientConfig
import io.pinecone.PineconeConnectionConfig
import javax.validation.Valid


class HeuristicsConfiguration(
    @Valid @JsonProperty("pineconeApiKeyName") val pineconeApiKeyName: String,
    @Valid @JsonProperty("serverSideTimeoutSeconds") val serverSideTimeoutSeconds: Int,
    @Valid @JsonProperty("serviceName") val serviceName: String,
    @Valid @JsonProperty("serviceAuthority") val serviceAuthority: String
) {
    fun getPineconeClientConfiguration(): PineconeClientConfig = PineconeClientConfig()
        .withServerSideTimeoutSec(serverSideTimeoutSeconds)

    fun getPineconeConnectionConfig(): PineconeConnectionConfig = PineconeConnectionConfig()
        .withServiceName(serviceName)
        .withServiceAuthority(serviceAuthority)
        .withSecure(true)
}