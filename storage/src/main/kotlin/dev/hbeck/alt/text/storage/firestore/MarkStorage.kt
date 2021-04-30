package dev.hbeck.alt.text.storage.firestore

import dev.hbeck.alt.text.proto.AltTextCoordinate


interface MarkStorage {
    fun incrementUsage(coordinate: AltTextCoordinate, increment: Long): Boolean
}