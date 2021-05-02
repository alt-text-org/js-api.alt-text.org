package dev.hbeck.alt.text.twitter

import dev.hbeck.alt.text.proto.TwitterAnalysis


interface TwitterAnalyzer {
    fun analyze(username: String): TwitterAnalysis?
}