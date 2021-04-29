package dev.hbeck.alt.text.storage.firestore


interface UsageMarker {
    fun markUsage(imgHash: String, userHash: String)
}