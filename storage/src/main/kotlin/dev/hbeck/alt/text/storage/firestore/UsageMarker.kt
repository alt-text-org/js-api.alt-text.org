package dev.hbeck.alt.text.storage.firestore

import dev.hbeck.alt.text.proto.AltTextCoordinate


interface UsageMarker {
    fun markUsage(coordinate: AltTextCoordinate)
}