package dev.hbeck.alt.text.storage.firestore

import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.inject.Inject
import com.google.inject.Singleton
import dev.hbeck.alt.text.common.toStringCoordinate
import dev.hbeck.alt.text.proto.AltTextCoordinate
import java.util.concurrent.TimeUnit


@Singleton
class FirestoreMarkStorage @Inject constructor(
    private val firestore: Firestore,
    private val configuration: FirestoreConfiguration
) : MarkStorage {
    override fun incrementUsage(coordinate: AltTextCoordinate, increment: Long): Boolean {
        val doc = firestore.collection(configuration.altTextCollection).document(coordinate.toStringCoordinate())

        val result = try {
            doc.update(mapOf(FirestoreAltTextStorage.usageField to FieldValue.increment(increment)))
                .get(configuration.writeAwaitMillis, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            return false
        }

        // Is this actually going to catch a write error? WHO CAN TELL, IT'S CERTAINLY NOT DOCUMENTED
        return result.updateTime.nanos > 0 && result.updateTime.seconds > 0
    }
}