package dev.hbeck.alt.text.twitter

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.inject.Inject


class TwitterCredRetriever @Inject constructor(
    private val secretClient: SecretManagerServiceClient,
    private val configuration: TwitterConfiguration
) {
    fun getApiKey() = getSecret(configuration.apiKeyName)
    fun getApiSecretKey() = getSecret(configuration.apiSecretKeyName)
    fun getAccessToken() = getSecret(configuration.accessTokenName)
    fun getAccessTokenSecret() = getSecret(configuration.accessTokenSecretName)

    private fun getSecret(name: String): String {
        val response = secretClient.accessSecretVersion(name)
        return response.payload.data.toStringUtf8()
    }
}