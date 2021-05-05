package dev.hbeck.alt.text

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.inject.AbstractModule


class GCPModule(private val configuration: AltTextConfiguration) : AbstractModule() {
    override fun configure() {
        val credentials = GoogleCredentials.getApplicationDefault()
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(configuration.firestoreConfig.projectId)
            .build()
        FirebaseApp.initializeApp(options)
        bind(Firestore::class.java).toInstance(FirestoreClient.getFirestore())

        val imageAnnotatorClient = ImageAnnotatorClient.create()
        bind(ImageAnnotatorClient::class.java).toInstance(imageAnnotatorClient)

        val secretManagerClient = SecretManagerServiceClient.create()
        bind(SecretManagerServiceClient::class.java).toInstance(secretManagerClient)
    }
}