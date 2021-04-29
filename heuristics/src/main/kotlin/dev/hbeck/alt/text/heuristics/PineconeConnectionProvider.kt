package dev.hbeck.alt.text.heuristics

import io.pinecone.PineconeConnection


interface PineconeConnectionProvider {
    fun getConnection(): PineconeConnection
}