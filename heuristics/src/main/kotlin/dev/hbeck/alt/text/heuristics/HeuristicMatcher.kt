package dev.hbeck.alt.text.heuristics


enum class HeuristicType(val vectorLength: Int?) {
    INTENSITY_HISTOGRAM(100);
}

interface HeuristicMatcher {
    fun matchHeuristic(type: HeuristicType, signature: String, language: String, matches: Int): List<Pair<String, Float>>

    fun addSignature(type: HeuristicType, imageHash: String, language: String, signature: String)
}