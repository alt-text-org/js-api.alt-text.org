package dev.hbeck.alt.text.twitter

import com.github.seratch.signedrequest4j.HttpException
import com.github.seratch.signedrequest4j.OAuthAccessToken
import com.github.seratch.signedrequest4j.OAuthConsumer
import com.github.seratch.signedrequest4j.SignedRequestFactory
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import java.io.StringReader
import java.time.Duration
import javax.ws.rs.BadRequestException
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

/**
 * None of the Twitter API clients out there let us set the options we need, but luckily we only need to implement
 * two requests: username -> user_id and get_user_timeline(user_id)
 */
class TwitterClient @Inject constructor(
    credRetriever: TwitterCredRetriever,
    configuration: TwitterConfiguration
): CacheLoader<String, User?>() {

    private val oAuthConsumer = OAuthConsumer(credRetriever.getApiKey(), credRetriever.getApiSecretKey())
    private val accessToken = OAuthAccessToken(credRetriever.getAccessToken(), credRetriever.getAccessTokenSecret())
    private val usernameCache = CacheBuilder.newBuilder()
        .maximumSize(configuration.usernameCacheTTLMinutes)
        .expireAfterWrite(Duration.ofMinutes(configuration.usernameCacheTTLMinutes))
        .build<String, User?>(this)

    fun usernameToUserId(username: String): User? {
        return usernameCache[username]
    }

    private fun usernameToUserIdDirect(username: String): User? {
        val users = sendRequest(
            "/users/lookup.json",
            object : TypeToken<List<User>>() {},
            "screen_name" to username
        )

        return when (users?.size) {
            1 -> users[0]
            0, null -> null
            else -> users.find { it.username == username }
        }
    }

    fun getUserTweets(userId: String): List<Tweet>? {
        return sendRequest(
            "/statuses/user_timeline.json",
            object : TypeToken<List<Tweet>>() {},
            "user_id" to userId,
            "count" to "200",
            "include_rts" to "false",
            "include_entities" to "true",
            "include_ext_alt_text" to "true",
            "tweet_mode:" to "extended"
        )
    }

    private fun <T : Any> sendRequest(
        endpoint: String,
        itemType: TypeToken<T>,
        vararg query: Pair<String, String>
    ): T? {
        val requestUrl = "https://api.twitter.com/1.1$endpoint?" +
                query.joinToString("&") { "${it.first}=${it.second}" }

        val request = SignedRequestFactory.create(oAuthConsumer, accessToken)

        return try {
            val response = request.doGet(requestUrl, Charsets.UTF_8.displayName())
            when (response.statusCode) {
                200 -> Gson().fromJson<T>(StringReader(response.textBody), itemType.type)
                403, 429 -> throw BadRequestException(Response.status(429, "Too Many Requests").build())
                else -> throw InternalServerErrorException("Failed fetch for '$requestUrl': ${response.statusCode}")
            }
        } catch (e: HttpException) {
            when (e.response.statusCode) {
                401 -> throw WebApplicationException(Response.status(403, "That account is locked").build())
                403, 429 -> throw WebApplicationException(Response.status(429, "Too Many Requests").build())
                else -> throw InternalServerErrorException("Failed fetch for '$requestUrl': ${e.response.statusCode}")
            }
        }
    }

    override fun load(key: String): User? {
        return usernameToUserIdDirect(key)
    }
}