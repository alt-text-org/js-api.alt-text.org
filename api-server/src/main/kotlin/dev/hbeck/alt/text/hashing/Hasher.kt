package dev.hbeck.alt.text.hashing


interface Hasher {
    fun hash(value: String): String
}