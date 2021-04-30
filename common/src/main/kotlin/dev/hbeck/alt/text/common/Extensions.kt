package dev.hbeck.alt.text.heuristics

import dev.hbeck.alt.text.proto.AltTextCoordinate


fun AltTextCoordinate.toStringCoordinate(): String {
    return "${this.imageHash}:${this.userHash}:${this.language}"
}

private val coordinateRegex = "(\\w{64}):(\\w{64}):(\\w{2})".toRegex()
fun AltTextCoordinate.Companion.fromStringCoordinate(str: String): AltTextCoordinate {
    val (imageHash, userHash, language) = coordinateRegex.find(str)?.destructured
        ?: throw RuntimeException("Couldn't parse '$str' as a text coordinate")

    return AltTextCoordinate(
        imageHash = imageHash,
        userHash = userHash,
        language = language
    )
}