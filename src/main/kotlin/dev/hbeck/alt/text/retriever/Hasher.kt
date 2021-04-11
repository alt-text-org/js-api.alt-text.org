package dev.hbeck.alt.text.retriever


interface Hasher {
    fun hash(value: String): String
}