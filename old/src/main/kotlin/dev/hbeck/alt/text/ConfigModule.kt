package dev.hbeck.alt.text

import com.google.inject.AbstractModule
import dev.hbeck.alt.text.http.auth.AuthConfiguration
import dev.hbeck.alt.text.storage.firestore.FirestoreConfiguration
import dev.hbeck.alt.text.twitter.TwitterConfiguration


class ConfigModule(private val config: AltTextConfiguration): AbstractModule() {
    override fun configure() {
        bind(AltTextConfiguration::class.java).toInstance(config)
        bind(TwitterConfiguration::class.java).toInstance(config.twitterConfig)
        bind(FirestoreConfiguration::class.java).toInstance(config.firestoreConfig)
        bind(AuthConfiguration::class.java).toInstance(config.authConfiguration)
    }
}