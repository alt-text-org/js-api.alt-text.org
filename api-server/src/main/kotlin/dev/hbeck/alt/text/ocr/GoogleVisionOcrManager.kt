package dev.hbeck.alt.text.ocr

import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageSource
import com.google.inject.Inject
import dev.hbeck.alt.text.proto.ExtractedText
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class GoogleVisionOcrManager @Inject constructor(
    private val visionClient: ImageAnnotatorClient
) : OcrManager {
    override fun attemptOcr(url: String): List<ExtractedText> {
        val image = Image.newBuilder()
            .setSource(ImageSource.newBuilder()
                .setImageUri(url)
                .build())
            .build()

        val feature = Feature.newBuilder()
            .setType(Feature.Type.LABEL_DETECTION)
            .build()

        val request = AnnotateImageRequest.newBuilder()
            .setImage(image)
            .addFeatures(feature)
            .build()

        val annotations = visionClient.batchAnnotateImages(listOf(request))?.responsesList ?: listOf()

        return annotations.flatMap { response ->
            if (response.hasError()) {
                log.error { "Got error from image annotation for url: ${url}: ${response.error}" }
                listOf()
            } else {
                response.labelAnnotationsList.map { annotation ->
                    ExtractedText(
                        language = annotation.locale,
                        text = annotation.description
                    )
                }
            }
        }
    }
}