package dev.hbeck.alt.text.lookup


interface SignatureParser {
    fun parseSignature(signature: String): FloatArray
}