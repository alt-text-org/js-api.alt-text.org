package dev.hbeck.alt.text.storage.firestore

import com.google.cloud.firestore.Firestore
import com.google.inject.Inject
import dev.hbeck.alt.text.proto.*
import dev.hbeck.alt.text.storage.AltTextWriteResult
import dev.hbeck.alt.text.storage.AltTextStorage
import dev.hbeck.alt.text.retriever.Hasher
import java.lang.IllegalStateException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class FirestoreAltTextStorage @Inject constructor(
    private val firestore: Firestore,
    private val usageMarker: AsyncUsageMarker,
    private val hasher: Hasher,
    private val configuration: FirestoreConfiguration
) : AltTextStorage {

    companion object {
        private val imgHashField = "img_hash"
        private val userHashField = "user_hash"
        private val usernameField = "username"
        private val altTextField = "alt_text"
        private val langField = "lang"
        private val urlField = "url"

        val usageField = "usage"
    }

    private val readExecutor = ThreadPoolExecutor(
        configuration.readThreads,
        configuration.readThreads,
        0,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(configuration.readQueueLen),
        ThreadPoolExecutor.CallerRunsPolicy()
    )

    override fun getAltTextForImage(imgHash: String, language: String?): Map<String, RetrievedAltText> {
        val collection = firestore.collection(configuration.altTextCollection)
        var query = collection.whereEqualTo(imgHashField, imgHash)
        if (language != null) {
            query = query.whereEqualTo(langField, language)
        }

        val result = query.get().get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)
        return result.documents.map {
            it[userHashField] as String to RetrievedAltText(
                text = it[altTextField] as String,
                language = it[langField] as String,
                timesUsed = it[usageField] as Long,
                confidence = 1.0F
            )
        }.toMap()
    }

    override fun getAltText(imgHash: String, userHash: String) =
        getAltTextAsync(imgHash, userHash).get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)

    private fun getAltTextAsync(imgHash: String, userHash: String): Future<UserAltText?> {
        val collection = firestore.collection(configuration.altTextCollection)
        val future = CompletableFuture<UserAltText?>()
        val apiFuture = collection.whereEqualTo(imgHashField, imgHash).whereEqualTo(userHashField, userHash).get()
        apiFuture.addListener({
            val result = apiFuture.get()
            when (val docs = result.documents.size) {
                1 -> {
                    val doc = result.documents[0]
                    future.complete(
                        UserAltText(
                            text = doc[altTextField] as String,
                            language = doc[langField] as String,
                            url = doc[urlField] as String? ?: "",
                            timesUsed = doc[usageField] as Long
                        )
                    )
                }
                0 -> future.complete(null)
                else -> future.completeExceptionally(IllegalStateException("Found $docs records for image '$imgHash' user '$userHash'"))
            }
        }, readExecutor)

        return future
    }

    override fun addAltTextAsync(
        imgHash: String,
        userHash: String,
        username: String,
        altText: String,
        language: String,
        url: String?
    ): AltTextWriteResult {
        val existing = getAltTextForImage(imgHash, language)
        existing.values.forEach {
            if (it.text == altText) {
                return AltTextWriteResult.CONFLICT
            }
        }

        val record = mutableMapOf<String, Any>()
        record[imgHashField] = imgHash
        record[userHashField] = userHash
        record[usernameField] = username
        record[altTextField] = altText
        record[langField] = language
        record[usageField] = 0
        url?.let { record[urlField] = it }

        val document = firestore.collection(configuration.altTextCollection).document("$imgHash:$userHash")
        document.set(record)
        return AltTextWriteResult.QUEUED
    }

    override fun deleteAltTextAsync(imgHash: String, username: String) {
        val userHash = hasher.hash(username)
        val document = firestore.collection(configuration.altTextCollection).document("$imgHash:$userHash")
        document.delete()
    }

    override fun getAltTextForUser(username: String): Map<String, UserAltText> {
        val userHash = hasher.hash(username)
        val collection = firestore.collection(configuration.altTextCollection)
        val query = collection.whereEqualTo(userHashField, userHash)

        val result = query.get().get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)
        return result.documents.map {
            it[imgHashField] as String to UserAltText(
                text = it[altTextField] as String,
                language = it[langField] as String,
                url = it[urlField] as String? ?: "",
                timesUsed = it[usageField] as Long
            )
        }.toMap()
    }

    override fun getFavoritesForUser(username: String): List<UserFavorite> {
        val collection = firestore.collection(configuration.faveCollection)
        val query = collection.whereEqualTo(usernameField, username)

        val result = query.get().get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)
        return result.documents.map {
            UserFavorite(
                userHash = it[userHashField] as String,
                imageHash = it[imgHashField] as String,
                text = it[altTextField] as String,
                language = it[langField] as String
            )
        }.toList()
    }

    override fun favoriteAsync(
        imgHash: String,
        userHash: String,
        username: String,
        altText: String,
        language: String
    ) {
        val record = mutableMapOf<String, Any>()
        record[imgHashField] = imgHash
        record[userHashField] = userHash
        record[usernameField] = username
        record[altTextField] = altText
        record[langField] = language

        val document = firestore.collection(configuration.faveCollection).document("$username:$imgHash:$userHash")
        document.set(record)
    }

    override fun markAltTextUsedAsync(imgHash: String, userHash: String) {
        usageMarker.markUsage(imgHash, userHash)
    }
}