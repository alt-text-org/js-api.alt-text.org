package dev.hbeck.alt.text.ocr

import dev.hbeck.alt.text.proto.ExtractedText


interface OcrManager {
    fun attemptOcr(url: String): List<ExtractedText>
}