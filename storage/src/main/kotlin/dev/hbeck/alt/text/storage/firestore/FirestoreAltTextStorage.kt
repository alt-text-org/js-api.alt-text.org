package dev.hbeck.alt.text.storage.firestore

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.QuerySnapshot
import com.google.inject.Inject
import dev.hbeck.alt.text.heuristics.toStringCoordinate
import dev.hbeck.alt.text.proto.*
import dev.hbeck.alt.text.storage.AltTextMutator
import dev.hbeck.alt.text.storage.AltTextRetriever
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

class FirestoreAltTextStorage @Inject constructor(
    private val firestore: Firestore,
    private val configuration: FirestoreConfiguration
) : AltTextMutator, AltTextRetriever {

    companion object {
        private const val imgHashField = "img_hash"
        private const val userHashField = "user_hash"
        private const val langField = "lang"
        private const val usernameField = "username"
        private const val authProviderField = "provider"
        private const val altTextField = "text"
        private const val urlField = "url"
        private const val publicField = "public"

        const val usageField = "usage"
    }

    override fun writeAltText(altText: InternalAltText): Boolean {
        val record = mutableMapOf<String, Any>()
        record[imgHashField] = altText.coordinate!!.imageHash
        record[userHashField] = altText.coordinate!!.userHash
        record[langField] = altText.coordinate!!.language
        record[usernameField] = altText.userInfo!!.username
        record[authProviderField] = altText.userInfo!!.provider
        record[altTextField] = altText.text
        record[urlField] = altText.url
        record[publicField] = altText.isPublic
        record[usageField] = 0

        return try {
            val document = firestore.collection(configuration.altTextCollection)
                .document(altText.coordinate!!.toStringCoordinate())
            val result = document.set(record).get(configuration.writeAwaitMillis, TimeUnit.MILLISECONDS)

            // Is this actually going to catch a write error? WHO CAN TELL, IT'S CERTAINLY NOT DOCUMENTED
            result.updateTime.nanos > 0 && result.updateTime.seconds > 0
        } catch (e: Exception) {
            log.error(e) { "Got exception attempting to write altText: $altText" }
            false
        }
    }

    override fun deleteAltText(coordinate: AltTextCoordinate): Boolean {
        return try {
            val document = firestore.collection(configuration.altTextCollection)
                .document(coordinate.toStringCoordinate())
            val result = document.delete().get(configuration.writeAwaitMillis, TimeUnit.MILLISECONDS)

            // Is this actually going to catch a write error? WHO CAN TELL, IT'S CERTAINLY NOT DOCUMENTED
            result.updateTime.nanos > 0 && result.updateTime.seconds > 0
        } catch (e: Exception) {
            log.error(e) { "Got exception attempting to delete altText at coordinate: $coordinate" }
            false
        }
    }

    override fun search(imageHash: String, language: String, matches: Int): List<InternalAltText> {
        val collection = firestore.collection(configuration.altTextCollection)
        val query = collection.whereEqualTo(imgHashField, imageHash).whereEqualTo(langField, language)

        val result = query.get().get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)
        return resultToAltTexts(result)
    }

    override fun getTextsForUser(userHash: String): List<InternalAltText> {
        val collection = firestore.collection(configuration.altTextCollection)
        val query = collection.whereEqualTo(userHashField, userHash)

        val result = query.get().get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)
        return resultToAltTexts(result)
    }

    override fun getAltText(coordinate: AltTextCoordinate): InternalAltText? {
        val collection = firestore.collection(configuration.altTextCollection)
        val query = collection.whereEqualTo(imgHashField, coordinate.imageHash)
            .whereEqualTo(userHashField, coordinate.userHash)
            .whereEqualTo(langField, coordinate.language)

        val result = query.get().get(configuration.readAwaitMillis, TimeUnit.MILLISECONDS)
        val texts = resultToAltTexts(result)

        return when (texts.size) {
            0 -> null
            1 -> texts[0]
            else -> throw RuntimeException("Expected at most one text for coordinate $coordinate but found ${texts.size}")
        }
    }

    private fun resultToAltTexts(result: QuerySnapshot): List<InternalAltText> {
        return result.documents.map {
            InternalAltText(
                coordinate = AltTextCoordinate(
                    imageHash = it[imgHashField] as String,
                    userHash = it[userHashField] as String,
                    language = it[langField] as String
                ), userInfo = SubmittingUserInfo(
                    username = it[usernameField] as String,
                    provider = it[authProviderField] as String
                ),
                url = it[urlField] as String,
                isPublic = it[publicField] as Boolean,
                text = it[altTextField] as String,
                timesUsed = it[usageField] as Long
            )
        }
    }
}