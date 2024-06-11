package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.query.connectors.OperationInvocationEventConsumer
import com.orbitalhq.schemas.RemoteOperation
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue

/**
 * A simple queue backed consumer of operationInvoked events.
 * Intended to be very fast in consuming events, so as not to block the calling thread.
 */
@Component
class QueueingOperationInvocationEventConsumer : OperationInvocationEventConsumer {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun drain(): List<OperationInvokedEvent> {
      val list = mutableListOf<OperationInvokedEvent>()
      queue.drainTo(list)
      return list
   }

   // Design choice:
   // Use a queue here, rather than a Sink / Flux, as we want lock-free writes from
   // many threads. A Sink will either fail, or block the producing thread on a busyLooping() loop.
   private val queue = LinkedBlockingQueue<OperationInvokedEvent>(Int.MAX_VALUE)
   override fun operationInvoked(operation: RemoteOperation) {
      if (!queue.offer(OperationInvokedEvent(operation))) {
         logger.warn { "Failed to count invocation of service. Is the queue full?" }
      }
   }

}

data class OperationInvokedEvent(val operation: RemoteOperation, val timestamp: Instant = Instant.now())
