package dev.hbeck.alt.text.http.ratelimits

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.util.concurrent.RateLimiter
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

@Suppress("UnstableApiUsage")
class RateLimit(limitPerSecond: Double, cacheSize: Int) {
    private val retryAfter = when {
        limitPerSecond >= 1 -> "1"
        else -> (1.0 / limitPerSecond).toInt().toString()
    }

    private val rateLimiters = CacheBuilder.newBuilder()
        .maximumSize(cacheSize.toLong())
        .build<String, RateLimiter>(Loader(limitPerSecond))

    fun checkRateLimit(scope: String) {
        val rateLimiter = rateLimiters[scope]
        if (!rateLimiter.tryAcquire()) {
            val response = Response.status(429, "Too Many Requests")
                .header("Retry-After", retryAfter).build()
            throw WebApplicationException(response)
        }
    }

    private class Loader(private val limitPerSecond: Double): CacheLoader<String, RateLimiter>() {
        override fun load(key: String): RateLimiter = RateLimiter.create(limitPerSecond)
    }
}