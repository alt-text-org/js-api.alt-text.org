package dev.hbeck.alt.text.http.auth

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid


class AuthConfiguration(
    @Valid @JsonProperty("admins") val admins: Set<String>,
    @Valid @JsonProperty("banned") val banned: Set<String>,
    @Valid @JsonProperty("acceptableTimeDeltaMillis") val acceptableTimeDeltaMillis: Long,
    @Valid @JsonProperty("google") val googleConfig: AuthProviderConfiguration,
    @Valid @JsonProperty("resourceRetrieverTimeoutSeconds") val resourceRetrieverTimeoutSeconds: Long
)
