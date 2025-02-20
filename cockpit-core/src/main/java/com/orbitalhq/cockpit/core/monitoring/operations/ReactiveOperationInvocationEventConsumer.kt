package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.query.connectors.OperationInvocationCountingEvent
import com.orbitalhq.query.connectors.OperationInvocationCountingEventConsumer
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * A simple reactive dispatcher for OperationInvocationCountingEventConsumer
 *
 * Exists because we have multiple different consumers - and this code exists on the hot path.
 * This means that slow consumers don't slow producers.
 */
@Component
class ReactiveOperationInvocationEventConsumer : OperationInvocationCountingEventConsumer {
   // Use a small, but not nothing, buffer
   private val operationInvokedSink =
      Sinks.many().multicast().onBackpressureBuffer<OperationInvocationCountingEvent>(50)
   private val operationEmittedResultSink =
      Sinks.many().multicast().onBackpressureBuffer<OperationInvocationCountingEvent>(50)
   private val operationThrewErrorSink =
      Sinks.many().multicast().onBackpressureBuffer<OperationInvocationCountingEvent>(50)

   val operationInvokedEvents: Flux<OperationInvocationCountingEvent> = operationInvokedSink.asFlux()
   val operationEmittedResultEvents: Flux<OperationInvocationCountingEvent> = operationEmittedResultSink.asFlux()
   val operationThrewEvents: Flux<OperationInvocationCountingEvent> = operationThrewErrorSink.asFlux()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private fun failureHandler(eventType: String): Sinks.EmitFailureHandler {
      return Sinks.EmitFailureHandler { _, emitResult ->
         logger.warn { "Operation metrics event dropped: eventType: $eventType, reason: $emitResult" }
         false
      }
   }

   override fun operationInvoked(event: OperationInvocationCountingEvent) {
      operationInvokedSink.emitNext(event, failureHandler("operationInvoked"))
   }

   override fun operationEmittedResult(event: OperationInvocationCountingEvent) {
      operationEmittedResultSink.emitNext(event, failureHandler("operationEmittedResult"))
   }

   override fun operationThrewError(event: OperationInvocationCountingEvent) {
      operationThrewErrorSink.emitNext(event, failureHandler("operationThrewError"))
   }
}
