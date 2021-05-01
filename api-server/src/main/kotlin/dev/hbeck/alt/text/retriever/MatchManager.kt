package dev.hbeck.alt.text.retriever

import dev.hbeck.alt.text.proto.Heuristics
import dev.hbeck.alt.text.proto.RetrievedAltText


interface MatchManager {
    fun getMatchingTexts(
        imageHash: String,
        heuristics: Heuristics?,
        language: String,
        matches: Int,
        includePrivate: Boolean
    ): List<RetrievedAltText>
}