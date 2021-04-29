package dev.hbeck.alt.text.twitter

enum class AltTextStatus {
    NO_IMAGE, ALT_TEXT, NO_ALT_TEXT
}

data class Tweet(val created_at: String, val extended_entities: Entities?)

data class Entities(val media: List<Media>?)

data class Media(val type: String, val ext_alt_text: String?)

data class User(val username: String, val id_str: String)