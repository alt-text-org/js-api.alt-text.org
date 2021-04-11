package dev.hbeck.alt.text.lookup

import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.*

class GoldbergSignatureParser : SignatureParser {
    private val signatureLength = 100
    private val expectedLen = Float.SIZE_BYTES * signatureLength

    override fun parseSignature(signature: String): FloatArray {
        val array = Base64.getDecoder().decode(signature)
        if (array.size != expectedLen) {
            throw RuntimeException("Got malformed Goldberg signature. expected $expectedLen bytes decoded but got ${array.size}")
        }

        val buffer = ByteBuffer.wrap(array)
        val parsed = FloatArray(signatureLength) { 0.0F }
        for (i in 0 until signatureLength) {
            parsed[i] = buffer.float
        }

        return parsed
    }
}