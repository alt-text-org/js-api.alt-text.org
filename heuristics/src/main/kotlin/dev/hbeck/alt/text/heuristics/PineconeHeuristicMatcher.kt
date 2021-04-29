package dev.hbeck.alt.text.heuristics

import com.google.inject.Inject
import io.pinecone.PineconeClient
import java.lang.RuntimeException


class PineconeHeuristicMatcher @Inject constructor(
    private val client: PineconeClient,
    private val connectionProvider: PineconeConnectionProvider,
    private val signatureParser: SignatureParser
) : HeuristicMatcher {

    override fun matchHeuristic(
        type: HeuristicType,
        signature: String,
        language: String,
        matches: Int
    ): List<Pair<String, Float>> {
        val signatureVector = signatureParser.parseSignature(signature, type.vectorLength)
        val requestVector = Array(1) { signatureVector }

        val request = client.queryRequest()
            .data(requestVector)
            .namespace(getNamespace(type, language))
            .includeData(false)
            .topK(matches)

        val response = connectionProvider.getConnection().send(request)
        return when (val responseSize = response.queryResults.size) {
            0 -> listOf()
            1 -> response.queryResults[0].ids.zip(response.queryResults[0].scores)
                .map { (id, distance) -> id to 1.0F - distance }
            else -> throw RuntimeException("Expected 0 or 1 query results, but got $responseSize")
        }
    }

    override fun addSignature(type: HeuristicType, imageHash: String, language: String, signature: String) {
        val signatureVector = signatureParser.parseSignature(signature, type.vectorLength)
        val requestVector = Array(1) { signatureVector }

        val request = client.upsertRequest()
            .data(requestVector)
            .ids(listOf(signature))
            .namespace(getNamespace(type, language))

        connectionProvider.getConnection().send(request)
    }

    private fun getNamespace(heuristicType: HeuristicType, language: String): String {
        val heuristicPrefix = when (heuristicType) {
            HeuristicType.INTENSITY_HISTOGRAM -> "IH"
        }

        return "${heuristicPrefix}_$language"
    }
}