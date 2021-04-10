package dev.hbeck.alt.text.storage

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.inject.AbstractModule
import dev.hbeck.alt.text.admin.AltTextAdmin
import dev.hbeck.alt.text.admin.LoggingAltTextAdmin
import dev.hbeck.alt.text.storage.firestore.AsyncUsageMarker
import dev.hbeck.alt.text.storage.firestore.FirestoreAltTextStorage
import dev.hbeck.alt.text.storage.firestore.FirestoreConfiguration
import dev.hbeck.alt.text.storage.firestore.FirestoreMarkStorage
import dev.hbeck.alt.text.storage.firestore.MarkStorage
import dev.hbeck.alt.text.storage.firestore.UsageMarker

class StorageModule(private val config: FirestoreConfiguration) : AbstractModule() {
    override fun configure() {
        val credentials = GoogleCredentials.getApplicationDefault()
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(config.projectId)
            .build()
        FirebaseApp.initializeApp(options)

        bind(Firestore::class.java).toInstance(FirestoreClient.getFirestore())
        bind(Hasher::class.java).toInstance(Blake3Hasher())

        bind(AltTextAdmin::class.java).to(LoggingAltTextAdmin::class.java)
        bind(UsageMarker::class.java).to(AsyncUsageMarker::class.java)
        bind(MarkStorage::class.java).to(FirestoreMarkStorage::class.java)
        bind(AltTextStorage::class.java).to(FirestoreAltTextStorage::class.java)
    }
}