package dev.hbeck.alt.text.heuristics

import io.pinecone.PineconeClient
import io.pinecone.PineconeConnection


interface PineconeProvider {
    fun getConnection(): PineconeConnection
    fun getClient(): PineconeClient
}