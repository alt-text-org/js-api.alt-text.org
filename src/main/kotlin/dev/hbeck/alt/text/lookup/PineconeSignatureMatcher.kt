package dev.hbeck.alt.text.lookup

import io.dropwizard.lifecycle.Managed
import io.pinecone.PineconeClient
import io.pinecone.PineconeConnection
import io.pinecone.PineconeConnectionConfig
import java.lang.RuntimeException


class PineconeSignatureMatcher(
    private val client: PineconeClient,
    private val connectionConfig: PineconeConnectionConfig,
    private val goldbergSignatureParser: SignatureParser
) : SignatureMatcher, Managed {

    private val goldbergNamespacePrefix = "gb-"

    private lateinit var connection: PineconeConnection;

    override fun getGoldbergMatches(signature: String, matches: Int, language: String): List<Pair<String, Float>> {
        val signatureVector = goldbergSignatureParser.parseSignature(signature)
        val requestVector = Array(1) { signatureVector }

        val request = client.queryRequest()
            .data(requestVector)
            .namespace(goldbergNamespacePrefix + language)
            .includeData(false)
            .topK(matches)

        val response = connection.send(request)
        return when (val responseSize = response.queryResults.size) {
            0 -> listOf()
            1 -> response.queryResults[0].ids.zip(response.queryResults[0].scores)
                .map { (id, distance) -> id to 1.0F - distance }
            else -> throw RuntimeException("Expected 0 or 1 query results, but got $responseSize")
        }
    }

    override fun addGoldbergSignature(identifier: String, signature: String, language: String) {
        val signatureVector = goldbergSignatureParser.parseSignature(signature)
        val requestVector = Array(1) { signatureVector }

        val request = client.upsertRequest()
            .data(requestVector)
            .ids(listOf(signature))
            .namespace(goldbergNamespacePrefix + language)

        connection.send(request)
    }

    override fun start() {
        connection = client.connect(connectionConfig)
    }

    override fun stop() {
        connection.close()
    }
}