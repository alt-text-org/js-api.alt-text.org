package dev.hbeck.alt.text.storage.firestore

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.hbeck.alt.text.proto.AltTextCoordinate
import java.io.Closeable
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Singleton
class AsyncUsageMarker @Inject constructor(
    private val markStorage: MarkStorage,
    private val configuration: FirestoreConfiguration
) : UsageMarker, Closeable {

    private val acceptMarks = AtomicBoolean(true)
    private val increments = AtomicReference<ConcurrentHashMap<AltTextCoordinate, AtomicLong>>(ConcurrentHashMap())
    private val flusher = Executors.newSingleThreadScheduledExecutor()
    private val flushLock = ReentrantLock()

    init {
        flusher.scheduleAtFixedRate(
            this::flush,
            configuration.markFlushIntervalSeconds,
            configuration.markFlushIntervalSeconds,
            TimeUnit.SECONDS
        )
    }

    override fun markUsage(coordinate: AltTextCoordinate) {
        if (!acceptMarks.get()) {
            throw IllegalStateException("Shutting down")
        }

        flushLock.withLock {
            increments.get().computeIfAbsent(coordinate) { AtomicLong(0) }.incrementAndGet()
        }
    }

    private fun flush() {
        val batch = flushLock.withLock {
            increments.getAndSet(ConcurrentHashMap())
        }

        batch.forEach { (coordinate, incr) ->
            val toIncrement = incr.getAndSet(0)
            if (!markStorage.incrementUsage(coordinate, toIncrement)) {
                incr.addAndGet(toIncrement)
            }
        }
    }

    override fun close() {
        acceptMarks.set(false)
        flusher.shutdown()
        flusher.awaitTermination(configuration.markShutdownAwaitSeconds, TimeUnit.SECONDS)
        flush()
    }
}