package dev.hbeck.alt.text.heuristics

import dev.hbeck.alt.text.proto.AltTextCoordinate

enum class HeuristicEncoding {
    BASE64, HEX_BITS
}

enum class HeuristicType(val vectorLength: Int?, val heuristicEncoding: HeuristicEncoding) {
    INTENSITY_HISTOGRAM(100, HeuristicEncoding.BASE64),
    AVERAGE_PERCEPTUAL_HASH(64, HeuristicEncoding.HEX_BITS),
    DCT_PERCEPTUAL_HASH(32, HeuristicEncoding.HEX_BITS)
}

interface HeuristicMatcher {
    fun matchHeuristic(type: HeuristicType, signature: String, language: String, matches: Int): Map<AltTextCoordinate, Float>

    fun addSignature(type: HeuristicType, coordinate: AltTextCoordinate, signature: String): Boolean
}