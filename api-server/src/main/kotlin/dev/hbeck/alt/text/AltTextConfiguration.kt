package dev.hbeck.alt.text

import com.fasterxml.jackson.annotation.JsonProperty
import dev.hbeck.alt.text.http.auth.AuthConfiguration
import dev.hbeck.alt.text.storage.firestore.FirestoreConfiguration
import dev.hbeck.alt.text.twitter.TwitterConfiguration
import io.dropwizard.Configuration
import javax.validation.Valid


class AltTextConfiguration(
    @Valid @JsonProperty("auth") val authConfiguration: AuthConfiguration,
    @Valid @JsonProperty("firestore") val firestoreConfig: FirestoreConfiguration,
    @Valid @JsonProperty("twitter") val twitterConfig: TwitterConfiguration,
    @Valid @JsonProperty("minTextLength") val minTextLength: Int,
    @Valid @JsonProperty("maxTextLength") val maxTextLength: Int
) : Configuration()