package dev.hbeck.alt.text.retriever

import dev.hbeck.alt.text.heuristics.HeuristicMatcher
import dev.hbeck.alt.text.heuristics.HeuristicType
import dev.hbeck.alt.text.proto.InternalAltText
import dev.hbeck.alt.text.proto.RetrievedAltText
import dev.hbeck.alt.text.storage.AltTextRetriever
import javax.inject.Inject


class DualMatchManager @Inject constructor(
    private val altTextRetriever: AltTextRetriever,
    private val heuristicMatcher: HeuristicMatcher
) : MatchManager {

    override fun getMatchingTexts(
        imageHash: String,
        signature: String,
        language: String,
        matches: Int
    ): List<RetrievedAltText> {
        val exactMatches = altTextRetriever.search(imageHash, language, matches)
            .map { internalTextToRetrieved(it, 1.0F) }

        if (exactMatches.size >= matches) {
            return exactMatches
        }

        val remainingToFetch = matches - exactMatches.size
        val intensityHistMatches = heuristicMatcher.matchHeuristic(
            type = HeuristicType.INTENSITY_HISTOGRAM,
            signature = signature,
            language = language,
            matches = remainingToFetch
        )

        val heuristicMatches: List<RetrievedAltText> = intensityHistMatches.map { (coordinate, distance) ->
            altTextRetriever.getAltText(coordinate)?.let { internalTextToRetrieved(it, distance) }
        }.filterNotNull()

        return exactMatches + heuristicMatches
    }

    private fun internalTextToRetrieved(internalAltText: InternalAltText, distance: Float): RetrievedAltText {
        return RetrievedAltText(
            imageHash = internalAltText.coordinate!!.imageHash,
            userHash = internalAltText.coordinate!!.userHash,
            language = internalAltText.coordinate!!.language,
            text = internalAltText.text,
            timesUsed = internalAltText.timesUsed,
            distance = distance
        )
    }
}