package dev.hbeck.alt.text.twitter


interface TwitterClient {
    fun usernameToUserId(username: String): User?
    fun getUserTweets(userId: String): List<Tweet>?
}