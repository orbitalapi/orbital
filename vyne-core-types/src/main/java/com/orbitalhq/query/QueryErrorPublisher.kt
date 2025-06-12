package com.orbitalhq.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Throwables
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.time.Instant

class StreamErrorPublisher : AutoCloseable {
   private val streamErrorsSink = Sinks.many()
      .replay()
      // Limit is quite short - we just want
      // to allow any late UI subscribers to get recent events
      .limit<QueryErrorEvent>(Duration.ofSeconds(30))

   val errors: Flux<QueryErrorEvent>
      get() = streamErrorsSink.asFlux()

   fun onError(queryId: String, error: StreamErrorMessage) {
      streamErrorsSink.emitNext(
         QueryErrorEvent(queryId, error),
         RetryFailOnSerializeEmitHandler
      )
   }

   override fun close() {
      streamErrorsSink.tryEmitComplete()
   }
}

class StreamErrorException(streamErrorMessage: StreamErrorMessage) : IllegalStateException(streamErrorMessage.exception)
data class StreamErrorMessage(
   val timestamp: Instant,
   @JsonIgnore
   val exception: Throwable,
   val message: String,
   val typeName: String,
   val payload: Any
) {
   companion object {
      fun fromException(ex: Exception, typeName: String): StreamErrorMessage {
         val rootCause = Throwables.getRootCause(ex)
         val rootCauseMessage = "A ${rootCause::class.simpleName} exception was thrown - ${rootCause.message ?: "No message provided"}."
         val message = if (rootCause is ExceptionWithFailedAttempts) {
            val failedAttempts = rootCause.failedAttempts.joinToString(separator = "; ") { it.toString() }
            "$rootCauseMessage - ${rootCause.failedAttempts.size} failed attempts were captured: $failedAttempts"
         } else rootCauseMessage
         return StreamErrorMessage(
            timestamp = Instant.now(),
            exception = ex,
            message = message,
            typeName = typeName,
            payload = message
         )
      }

      fun fromThrowable(throwable: Throwable, typeName: String): StreamErrorMessage {
         return if (throwable is Exception) {
            fromException(throwable, typeName)
         } else {
            fromException(RuntimeException(throwable), typeName)
         }
      }

   }

   fun toException(): StreamErrorException = StreamErrorException(this)
}

data class QueryErrorEvent(val queryId: String, val error: StreamErrorMessage, val tags: MetricTags = MetricTags.NONE)
