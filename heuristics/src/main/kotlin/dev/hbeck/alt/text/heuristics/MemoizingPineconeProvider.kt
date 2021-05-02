package dev.hbeck.alt.text.heuristics

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.inject.Inject
import io.pinecone.PineconeClient
import io.pinecone.PineconeConnection


class MemoizingPineconeProvider @Inject constructor(
    private val config: HeuristicsConfiguration,
    private val secretClient: SecretManagerServiceClient
) : PineconeProvider {

    private val conn: PineconeConnection by lazy {
        PineconeConnection(config.getPineconeClientConfiguration(), config.getPineconeConnectionConfig())
    }

    private val cpClient: PineconeClient by lazy {
        val secretResponse = secretClient.accessSecretVersion(config.pineconeApiKeyName)
        val key = secretResponse.payload.data.toStringUtf8()

        val pcClientConfig = config.getPineconeClientConfiguration()
            .withApiKey(key)

        PineconeClient(pcClientConfig)
    }

    override fun getConnection(): PineconeConnection = conn

    override fun getClient(): PineconeClient = cpClient
}