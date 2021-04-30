package dev.hbeck.alt.text.mutation

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid


class MutationHandlerConfiguration(
    @Valid @JsonProperty("queueLength") val mutationQueueLength: Int,
    @Valid @JsonProperty("queueWorkers") val mutationQueueWorkers: Int,
    @Valid @JsonProperty("workerSleepMillis") val workerSleepMillis: Long,
    @Valid @JsonProperty("shutdownAwaitSeconds") val shutdownAwaitSeconds: Long,
    @Valid @JsonProperty("writeAttempts") val writeAttempts: Int,
    @Valid @JsonProperty("requeueWaitSeconds") val requeueWaitSeconds: Long,
    @Valid @JsonProperty("backoffMillis") val backoffMillis: Long
)