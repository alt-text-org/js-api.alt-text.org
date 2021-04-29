package dev.hbeck.alt.text.heuristics

import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.*


class SignatureParser {
    fun parseSignature(signature: String, signatureLength: Int?): FloatArray {
        val array = Base64.getDecoder().decode(signature)
        signatureLength?.let {
            if (array.size != it * Float.SIZE_BYTES) {
                throw RuntimeException("Got malformed signature. expected ${it * Float.SIZE_BYTES} bytes decoded but got ${array.size}")
            }
        }

        if (array.size % Float.SIZE_BYTES != 0) {
            throw RuntimeException("Got malformed signature. Signature length ${array.size} is not divisible by ${Float.SIZE_BYTES}")
        }

        val buffer = ByteBuffer.wrap(array)
        val parsed = FloatArray(array.size / Float.SIZE_BYTES) { 0.0F }
        for (i in 0 until array.size / Float.SIZE_BYTES) {
            parsed[i] = buffer.float
        }

        return parsed
    }
}