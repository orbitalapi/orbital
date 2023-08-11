package io.vyne.utils

import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks

inline fun <reified O> Flux<*>.filterIsInstance(): Flux<O> {
   return this.mapNotNull { message ->
      if (message is O) {
         message as O
      } else {
         null
      }
   }
}


object RetryFailOnSerializeEmitHandler : Sinks.EmitFailureHandler {
   override fun onEmitFailure(signalType: SignalType, emitResult: Sinks.EmitResult) =
      emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
}
