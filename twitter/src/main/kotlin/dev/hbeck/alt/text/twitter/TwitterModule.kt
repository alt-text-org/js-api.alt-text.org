package dev.hbeck.alt.text.twitter

import com.google.inject.AbstractModule


class TwitterModule(private val configuration: TwitterConfiguration) : AbstractModule() {
    override fun configure() {
        bind(TwitterClient::class.java).to(CustomTwitterClient::class.java)
        bind(TwitterAnalyzer::class.java).to(DirectTwitterAnalyzer::class.java)
        bind(TwitterCredRetriever::class.java).to(GCPTwitterCredRetriever::class.java)
    }
}