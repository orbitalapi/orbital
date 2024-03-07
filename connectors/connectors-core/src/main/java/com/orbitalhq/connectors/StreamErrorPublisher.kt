package com.orbitalhq.connectors

import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.time.Instant

class StreamErrorPublisher {
   private val streamErrorsSink = Sinks.many()
      .replay()
      // Limit is quite short - we just want
      // to allow any late UI subscribers to get recent events
      .limit<StreamQueryErrorEvent>(Duration.ofSeconds(30))

   val errors: Flux<StreamQueryErrorEvent>
      get() = streamErrorsSink.asFlux()

   fun onError(queryId: String, error: StreamErrorMessage) {
      streamErrorsSink.emitNext(
         StreamQueryErrorEvent(queryId, error),
         RetryFailOnSerializeEmitHandler
      )
   }
}

data class StreamErrorMessage(
   val timestamp: Instant,
   @JsonIgnore
   val exception: Exception,
   val message: String,
   val typeName: String,
   val payload: Any
)
data class StreamQueryErrorEvent(val queryId: String, val error: StreamErrorMessage)
