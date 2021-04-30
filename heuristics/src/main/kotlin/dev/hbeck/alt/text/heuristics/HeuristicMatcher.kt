package dev.hbeck.alt.text.heuristics

import dev.hbeck.alt.text.proto.AltTextCoordinate


enum class HeuristicType(val vectorLength: Int?) {
    INTENSITY_HISTOGRAM(100);
}

interface HeuristicMatcher {
    fun matchHeuristic(type: HeuristicType, signature: String, language: String, matches: Int): Map<AltTextCoordinate, Float>

    fun addSignature(type: HeuristicType, coordinate: AltTextCoordinate, signature: String): Boolean
}