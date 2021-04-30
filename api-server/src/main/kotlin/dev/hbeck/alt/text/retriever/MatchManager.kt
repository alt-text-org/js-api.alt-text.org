package dev.hbeck.alt.text.retriever

import dev.hbeck.alt.text.proto.RetrievedAltText


interface MatchManager {
    fun getMatchingTexts(
        imageHash: String,
        signature: String,
        language: String,
        matches: Int
    ): List<RetrievedAltText>
}