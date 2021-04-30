package dev.hbeck.alt.text.storage.firestore

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid


class FirestoreConfiguration(
    @Valid @JsonProperty("projectId") val projectId: String,
    @Valid @JsonProperty("databaseId") val databaseId: String,
    @Valid @JsonProperty("altTextCollection") val altTextCollection: String,
    @Valid @JsonProperty("readAwaitMillis") val readAwaitMillis: Long,
    @Valid @JsonProperty("writeAwaitMillis") val writeAwaitMillis: Long,
    @Valid @JsonProperty("markFlushIntervalSeconds") val markFlushIntervalSeconds: Long,
    @Valid @JsonProperty("markShutdownAwaitSeconds") val markShutdownAwaitSeconds: Long
)