package dev.hbeck.alt.text.admin



interface AltTextAdmin {
    fun report(imgHash: String, usernameHash: String, username: String, reason: String)
}