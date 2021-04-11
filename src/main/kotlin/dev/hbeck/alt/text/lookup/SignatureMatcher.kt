package dev.hbeck.alt.text.lookup


interface SignatureMatcher {
    fun getGoldbergMatches(signature: String, matches: Int, language: String): List<Pair<String, Float>>

    fun addGoldbergSignature(identifier: String, signature: String, language: String)
}