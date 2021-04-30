package dev.hbeck.alt.text.retriever

import dev.hbeck.alt.text.lookup.SignatureMatcher
import dev.hbeck.alt.text.proto.RetrievedAltText
import dev.hbeck.alt.text.storage.AltTextStorage
import javax.inject.Inject


class DualMatchManager @Inject constructor(
    private val signatureMatcher: SignatureMatcher,
    private val altTextStorage: AltTextStorage,
    private val hasher: Hasher
) : MatchManager {

    override fun getMatchingTexts(
        imageHash: String,
        signature: String,
        language: String,
        matches: Int
    ): Map<String, RetrievedAltText> {
        val altTextForImage = altTextStorage.getAltTextForImage(imageHash, language)
        if (altTextForImage.size >= matches) {
            return altTextForImage
        }

        val remainingToFetch = matches - altTextForImage.size
        val goldbergMatches = signatureMatcher.getGoldbergMatches(
            signature = signature,
            matches = remainingToFetch,
            language = language
        )

        if (goldbergMatches.isNotEmpty()) {
            val mutableMatches = altTextForImage.toMutableMap()
            goldbergMatches.forEach { (identifier, confidence) ->
                val (foundImageHash, userHash) = identiferFormat.find(identifier)?.destructured
                    ?: throw RuntimeException("Got malformed identifier from matcher: $identifier")
                altTextStorage.getAltText(foundImageHash, userHash)?.apply {
                    mutableMatches[userHash] = RetrievedAltText(
                        text = text,
                        language = language,
                        confidence = confidence,
                        timesUsed = timesUsed
                    )
                }
            }
            return mutableMatches
        } else {
            return altTextForImage
        }
    }

    override fun addAltTextMatch(
        imageHash: String,
        username: String,
        altText: String,
        url: String?,
        signature: String,
        language: String
    ) {
        val usernameHash = hasher.hash(username)
        altTextStorage.addAltTextAsync(
            imgHash = imageHash,
            userHash = usernameHash,
            username = username,
            altText = altText,
            language = language,
            url = url
        )

        signatureMatcher.addGoldbergSignature("$imageHash:$usernameHash", signature, language)
    }
}