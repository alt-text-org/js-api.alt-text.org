package dev.hbeck.alt.text.storage

import com.google.inject.Singleton
import io.lktk.NativeBLAKE3
import org.apache.commons.codec.binary.Hex

@Singleton
class Blake3Hasher : Hasher {

//    companion object {
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val hasher = Blake3Hasher()
//            System.err.println(hasher.hash("Hello world"))
//        }
//    }

    init {
        if (!NativeBLAKE3.isEnabled()) {
            throw IllegalStateException("Blake3 not enabled!")
        }
    }

    override fun hash(value: String): String {
        val hasher = NativeBLAKE3()
        try {
            hasher.initDefault()
            hasher.update(value.toByteArray())
            return Hex.encodeHexString(hasher.output)
        } finally {
            hasher.close()
        }
    }
}