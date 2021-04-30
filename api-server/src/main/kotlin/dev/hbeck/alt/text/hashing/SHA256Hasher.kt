package dev.hbeck.alt.text.hashing

import com.google.common.hash.Hashing


@Suppress("UnstableApiUsage")
class SHA256Hasher : Hasher {
    override fun hash(value: String): String = Hashing.sha256().hashString(value, Charsets.UTF_8).toString()
}