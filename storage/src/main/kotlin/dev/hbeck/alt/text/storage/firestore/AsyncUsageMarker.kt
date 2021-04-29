package dev.hbeck.alt.text.storage.firestore

import com.google.inject.Inject
import com.google.inject.Singleton
import io.dropwizard.lifecycle.Managed
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Singleton
class AsyncUsageMarker @Inject constructor(
    private val markStorage: MarkStorage,
    private val configuration: FirestoreConfiguration
) : Managed, UsageMarker {

    private data class AltTextIdentifier(val imgHash: String, val userHash: String)

    private val acceptMarks = AtomicBoolean(true)
    private val increments = ConcurrentHashMap<AltTextIdentifier, AtomicLong>()
    private val flusher = Executors.newSingleThreadScheduledExecutor()

    override fun markUsage(imgHash: String, userHash: String) {
        val id = AltTextIdentifier(imgHash, userHash)
        increments.computeIfAbsent(id) { AtomicLong(0) }.incrementAndGet()
    }

    fun flush() {
        increments.forEach { (id, incr) ->
            val toIncrement = incr.getAndSet(0)
            if (!markStorage.incrementUsage(id.imgHash, id.userHash, toIncrement)) {
                incr.addAndGet(toIncrement)
            }
        }
    }

    override fun start() {
        flusher.scheduleAtFixedRate(
            this::flush,
            configuration.markFlushIntervalSeconds,
            configuration.markFlushIntervalSeconds,
            TimeUnit.SECONDS
        )
    }

    override fun stop() {
        acceptMarks.set(false)
        flusher.shutdown()
        flusher.awaitTermination(configuration.markShutdownAwaitSeconds, TimeUnit.SECONDS)
        flush()
    }
}