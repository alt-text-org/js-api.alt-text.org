package dev.hbeck.alt.text.retriever

import dev.hbeck.alt.text.heuristics.HeuristicMatcher
import dev.hbeck.alt.text.heuristics.HeuristicType
import dev.hbeck.alt.text.proto.Heuristics
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
        heuristics: Heuristics?,
        language: String,
        matches: Int,
        includePrivate: Boolean
    ): List<RetrievedAltText> {
        var matchesFound = altTextRetriever.searchExact(imageHash, language, matches)
            .filter { includePrivate || it.isPublic }
            .map { internalTextToRetrieved(it, 1.0F) }

        if (matchesFound.size >= matches) {
            return matchesFound
        }

        val intensityHisMatches: List<RetrievedAltText> =
            if (heuristics != null && heuristics.intensityHist.isNotEmpty()) {
                val remainingToFetch = matches - matchesFound.size
                val intensityHistMatches = heuristicMatcher.matchHeuristic(
                    type = HeuristicType.INTENSITY_HISTOGRAM,
                    signature = heuristics.intensityHist,
                    language = language,
                    matches = remainingToFetch
                )

                intensityHistMatches.map { (coordinate, distance) ->
                    altTextRetriever.getAltText(coordinate)?.takeIf { includePrivate || it.isPublic }
                        ?.let { internalTextToRetrieved(it, distance) }
                }.filterNotNull()
            }

        return matchesFound + intensityHisMatches
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