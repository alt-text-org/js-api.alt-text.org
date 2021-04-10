package dev.hbeck.alt.text.storage

import dev.hbeck.alt.text.proto.ImageRecord
import dev.hbeck.alt.text.proto.UserFavorite
import dev.hbeck.alt.text.proto.UserImageRecord


interface AltTextStorage {
    fun getAltTextForImage(imgHash: String, languages: Set<String>): Map<String, ImageRecord>

    fun getAltText(imgHash: String, userHash: String): UserImageRecord?

    fun addAltTextAsync(
        imgHash: String,
        username: String,
        altText: String,
        language: String,
        url: String?
    ): AltTextWriteResult

    fun deleteAltTextAsync(imgHash: String, username: String)

    fun getAltTextForUser(username: String): Map<String, UserImageRecord>

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