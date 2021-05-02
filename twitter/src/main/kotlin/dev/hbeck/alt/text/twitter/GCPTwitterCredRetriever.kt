package dev.hbeck.alt.text.twitter

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.inject.Inject


class GCPTwitterCredRetriever @Inject constructor(
    private val secretClient: SecretManagerServiceClient,
    private val configuration: TwitterConfiguration
) : TwitterCredRetriever {
    override fun getApiKey() = getSecret(configuration.apiKeyName)
    override fun getApiSecretKey() = getSecret(configuration.apiSecretKeyName)
    override fun getAccessToken() = getSecret(configuration.accessTokenName)
    override fun getAccessTokenSecret() = getSecret(configuration.accessTokenSecretName)

    private fun getSecret(name: String): String {
        val response = secretClient.accessSecretVersion(name)
        return response.payload.data.toStringUtf8()
    }
}