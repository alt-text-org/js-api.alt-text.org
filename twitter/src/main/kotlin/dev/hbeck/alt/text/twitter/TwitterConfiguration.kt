package dev.hbeck.alt.text.twitter

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid


data class TwitterConfiguration(
    @Valid @JsonProperty("accessTokenName") val accessTokenName: String,
    @Valid @JsonProperty("accessTokenSecretName") val accessTokenSecretName: String,
    @Valid @JsonProperty("apiKeyName") val apiKeyName: String,
    @Valid @JsonProperty("apiSecretKeyName") val apiSecretKeyName: String,
    @Valid @JsonProperty("usernameCacheSize") val usernameCacheSize: Int,
    @Valid @JsonProperty("usernameCacheTTLMinutes") val usernameCacheTTLMinutes: Long

)