package dev.hbeck.alt.text.heuristics

import com.google.inject.AbstractModule


class HeuristicsModule(private val config: HeuristicsConfiguration) : AbstractModule() {
    override fun configure() {
        bind(PineconeProvider::class.java).to(MemoizingPineconeProvider::class.java)
        bind(SignatureParser::class.java).toInstance(SignatureParser())
        bind(HeuristicMatcher::class.java).to(PineconeHeuristicMatcher::class.java)
    }
}