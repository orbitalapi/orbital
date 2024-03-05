package com.orbitalhq.connectors

import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class StreamErrorPublisher {
   private val streamErrorsSink = Sinks.many().unicast().onBackpressureBuffer<StreamError>()

   val errors: Flux<StreamError>
      get() = streamErrorsSink.asFlux()

   fun onError(queryId: String, error: Exception) {
      streamErrorsSink.emitNext(
         StreamError(queryId, error),
         RetryFailOnSerializeEmitHandler
      )
   }
}

data class StreamError(val queryId: String, val error: Exception)
