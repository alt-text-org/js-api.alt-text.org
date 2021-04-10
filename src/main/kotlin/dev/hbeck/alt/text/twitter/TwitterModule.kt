package dev.hbeck.alt.text.twitter

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.inject.AbstractModule


class TwitterModule(): AbstractModule() {
    override fun configure() {
        bind(SecretManagerServiceClient::class.java).toInstance(SecretManagerServiceClient.create())
    }
}