package dev.hbeck.alt.text.twitter


interface TwitterCredRetriever {
    fun getApiKey(): String
    fun getApiSecretKey(): String
    fun getAccessToken(): String
    fun getAccessTokenSecret(): String
}