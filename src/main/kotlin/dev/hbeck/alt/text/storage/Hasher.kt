package dev.hbeck.alt.text.storage


interface Hasher {
    fun hash(value: String): String
}