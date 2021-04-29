package dev.hbeck.alt.text.storage.firestore


interface MarkStorage {
    fun incrementUsage(imgHash: String, userHash: String, increment: Long): Boolean
}