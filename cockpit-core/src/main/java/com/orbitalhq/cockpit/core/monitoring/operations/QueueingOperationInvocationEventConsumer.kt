package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.query.connectors.OperationInvocationCountingEvent
import com.orbitalhq.query.connectors.OperationInvocationCountingEventConsumer
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue

/**
 * A simple queue backed consumer of operationInvoked events.
 * Intended to be very fast in consuming events, so as not to block the calling thread.
 *
 * It then dispatches back out to reactive fluxes, to allow multiple consumers.
 *
 * This exists because Flux / Sink doesn't offer a fast non-blocking sink
 * that supports multiple proudcers from different threads.
 */
@Primary // Multiple exist - but this one acts as a non-blocking broker, to then send events off to the ReactiveOperationInvocationEventConsumer in a single-threaded producer
@Component
class QueueingOperationInvocationEventConsumer(private val reactiveOperationInvocationEventConsumer: ReactiveOperationInvocationEventConsumer,
                                               private val maxQueueSize:Int = 5_000,
   private val pollDuration: Duration = Duration.ofSeconds(1)

) : OperationInvocationCountingEventConsumer {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
       Flux.interval(pollDuration)
          .subscribe {
             drain(invokeEvents, reactiveOperationInvocationEventConsumer::operationInvoked)
             drain(resultEvents, reactiveOperationInvocationEventConsumer::operationEmittedResult)
             drain(errorEvents, reactiveOperationInvocationEventConsumer::operationThrewError)
          }
   }

   private fun drain(queue: LinkedBlockingQueue<OperationInvocationCountingEvent>, handler: (OperationInvocationCountingEvent) -> Unit) {
      val list = mutableListOf<OperationInvocationCountingEvent>()
      queue.drainTo(list)
      list.forEach { handler(it) }
   }


   // Design choice:
   // Use a queue here, rather than a Sink / Flux, as we want lock-free writes from
   // many threads. A Sink will either fail, or block the producing thread on a busyLooping() loop.
   private val invokeEvents = LinkedBlockingQueue<OperationInvocationCountingEvent>(maxQueueSize)
   private val resultEvents = LinkedBlockingQueue<OperationInvocationCountingEvent>(maxQueueSize)
   private val errorEvents = LinkedBlockingQueue<OperationInvocationCountingEvent>(maxQueueSize)

   override fun operationInvoked(event: OperationInvocationCountingEvent) {
      if (!invokeEvents.offer(event)) {
         logger.warn { "Failed to count invocation of service. Is the queue full?" }
      }
   }

   override fun operationEmittedResult(event: OperationInvocationCountingEvent) {
      if (!resultEvents.offer(event)) {
         logger.warn { "Failed to count result emission from an operation. Is the queue full?" }
      }
   }

   override fun operationThrewError(event: OperationInvocationCountingEvent) {
      if (!errorEvents.offer(event)) {
         logger.warn { "Failed to count error emission from an operation. Is the queue full?" }
      }
   }

}


