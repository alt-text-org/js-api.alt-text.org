package dev.hbeck.alt.text.storage

import dev.hbeck.alt.text.proto.*


interface AltTextStorage {
    fun getAltTextForImage(imgHash: String, language: String?): Map<String, RetrievedAltText>

    fun getAltText(imgHash: String, userHash: String): UserAltText?

    fun addAltTextAsync(
        imgHash: String,
        userHash: String,
        username: String,
        altText: String,
        language: String,
        url: String?
    ): AltTextWriteResult

    fun deleteAltTextAsync(imgHash: String, username: String)

    fun getAltTextForUser(username: String): Map<String, UserAltText>

    fun getFavoritesForUser(username: String): List<UserFavorite>

    fun favoriteAsync(
        imgHash: String,
        userHash: String,
        username: String,
        altText: String,
        language: String
    )

    fun markAltTextUsedAsync(imgHash: String, userHash: String)
}