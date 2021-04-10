package dev.hbeck.alt.text.twitter

import com.google.inject.Inject
import dev.hbeck.alt.text.proto.TwitterAnalysis
import java.util.concurrent.atomic.AtomicInteger

class TwitterAnalyzer @Inject constructor(
    private val twitterClient: TwitterClient
) {

    fun analyze(username: String): TwitterAnalysis? {
        val userId = twitterClient.usernameToUserId(username) ?: return null
        val tweets = twitterClient.getUserTweets(userId.id_str) ?: return TwitterAnalysis(0, 0, 0, 0)
        val counts = HashMap<AltTextStatus, AtomicInteger>()
        tweets.forEach {
            counts.computeIfAbsent(getAltTextStatus(it)) { AtomicInteger(0) }.incrementAndGet()
        }

        return TwitterAnalysis(
            tweets = tweets.size,
            hasAltText = counts[AltTextStatus.ALT_TEXT]?.get() ?: 0,
            noAltText = counts[AltTextStatus.NO_ALT_TEXT]?.get() ?: 0,
            noImage = counts[AltTextStatus.NO_IMAGE]?.get() ?: 0,
            since = tweets.lastOrNull()?.created_at ?: ""
        )
    }

    private fun getAltTextStatus(tweet: Tweet): AltTextStatus {
        val photos = tweet.extended_entities?.media?.filter { it.type == "photo" || it.type == "animate_gif" }
            ?.takeIf { it.isNotEmpty() } ?: return AltTextStatus.NO_IMAGE

        return if (photos.all { it.ext_alt_text?.takeIf { alt -> alt.isNotEmpty() } != null }) {
            AltTextStatus.ALT_TEXT
        } else {
            AltTextStatus.NO_ALT_TEXT
        }
    }
}