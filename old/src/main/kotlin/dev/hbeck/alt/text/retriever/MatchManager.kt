package dev.hbeck.alt.text.retriever

import dev.hbeck.alt.text.proto.RetrievedAltText


interface MatchManager {
    fun getMatchingTexts(
        imageHash: String,
        signature: String,
        language: String,
        matches: Int
    ): Map<String, RetrievedAltText>

    fun addAltTextMatch(imageHash: String, username: String, altText: String, url:String?, signature: String, language: String)
}