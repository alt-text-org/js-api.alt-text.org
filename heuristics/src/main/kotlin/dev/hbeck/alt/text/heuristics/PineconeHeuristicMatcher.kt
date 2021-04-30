package dev.hbeck.alt.text.heuristics

import com.google.inject.Inject
import dev.hbeck.alt.text.proto.AltTextCoordinate
import io.pinecone.PineconeClient
import mu.KotlinLogging
import java.lang.Exception
import java.lang.RuntimeException

val log = KotlinLogging.logger{}

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
    ): Map<AltTextCoordinate, Float> {
        val signatureVector = signatureParser.parseSignature(signature, type.vectorLength)
        val requestVector = Array(1) { signatureVector }

        val request = client.queryRequest()
            .data(requestVector)
            .namespace(getNamespace(type, language))
            .includeData(false)
            .topK(matches)

        val response = connectionProvider.getConnection().send(request)
        return when (val responseSize = response.queryResults.size) {
            0 -> mapOf()
            1 -> response.queryResults[0].ids.zip(response.queryResults[0].scores)
                .associate { (id, distance) -> AltTextCoordinate.fromStringCoordinate(id) to 1.0F - distance }
            else -> throw RuntimeException("Expected 0 or 1 query results, but got $responseSize")
        }
    }

    override fun addSignature(type: HeuristicType, coordinate: AltTextCoordinate, signature: String): Boolean {
        val signatureVector = signatureParser.parseSignature(signature, type.vectorLength)

        val request = client.upsertRequest()
            .data(Array(1) { signatureVector })
            .ids(listOf(coordinate.toStringCoordinate()))
            .namespace(getNamespace(type, coordinate.language))

        return try {
            connectionProvider.getConnection().send(request)
            true;
        } catch (e: Exception) {
            log.error(e) { "Failed to write signature for coordinate $coordinate and heuristic type $type" }
            false
        }
    }

    private fun getNamespace(heuristicType: HeuristicType, language: String): String {
        val heuristicPrefix = when (heuristicType) {
            HeuristicType.INTENSITY_HISTOGRAM -> "IH"
        }

        return "${heuristicPrefix}_$language"
    }
}