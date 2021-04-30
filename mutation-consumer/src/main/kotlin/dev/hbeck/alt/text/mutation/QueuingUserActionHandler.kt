package dev.hbeck.alt.text.mutation

import com.google.inject.Inject
import com.google.inject.name.Named
import dev.hbeck.alt.text.proto.UserActionEvent
import mu.KotlinLogging
import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

class QueuingUserActionHandler @Inject constructor(
    @Named("queuingHandler") actualHandler: UserActionHandler,
    private val configuration: MutationHandlerConfiguration
) : UserActionHandler, Closeable {

    private val keepRunning = AtomicBoolean(true)
    private val queue = LinkedBlockingQueue<RetryWrapper>(configuration.mutationQueueLength)
    private val handlers = Executors.newFixedThreadPool(configuration.mutationQueueWorkers)

    init {
        for (i in 0 until configuration.mutationQueueWorkers) {
            handlers.submit(
                Handler(
                    queue = queue,
                    actionHandler = actualHandler,
                    keepRunning = keepRunning,
                    sleepMillis = configuration.workerSleepMillis,
                    requeueSeconds = configuration.requeueWaitSeconds,
                    attempts = configuration.writeAttempts,
                    backoffMillis = configuration.backoffMillis
                )
            )
        }
    }

    override fun handleEvent(event: UserActionEvent): Boolean {
        return queue.offer(RetryWrapper(event, AtomicInteger(0)))
    }

    override fun close() {
        keepRunning.set(false)
        handlers.awaitTermination(configuration.shutdownAwaitSeconds, TimeUnit.SECONDS)
    }

    private data class RetryWrapper(val event: UserActionEvent, val attempt: AtomicInteger)

    private class Handler(
        private val queue: BlockingQueue<RetryWrapper>,
        private val actionHandler: UserActionHandler,
        private val keepRunning: AtomicBoolean,
        private val sleepMillis: Long,
        private val attempts: Int,
        private val requeueSeconds: Long,
        private val backoffMillis: Long
    ) : Runnable {
        override fun run() {
            while (keepRunning.get() || queue.size > 0) {
                val wrapper = queue.poll(sleepMillis, TimeUnit.MILLISECONDS)
                if (wrapper != null) {
                    val success = try {
                        actionHandler.handleEvent(wrapper.event)
                    } catch (e: Exception) {
                        log.error(e) { "Exception handling event: $wrapper" }
                        false
                    }

                    if (!success) {
                        if (keepRunning.get()) {
                            Thread.sleep(backoffMillis * wrapper.attempt.get())
                        }

                        wrapper.attempt.incrementAndGet()
                        if (wrapper.attempt.get() >= attempts) {
                            log.error { "Event write exhausted retries, dropping: ${wrapper.event}" }
                            continue
                        }

                        if (!queue.offer(wrapper, requeueSeconds, TimeUnit.SECONDS)) {
                            log.error { "Unable to requeue event, dropping: $wrapper" }
                        }
                    }
                }
            }
        }
    }
}