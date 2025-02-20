package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.query.connectors.OperationInvocationCountingEvent
import com.orbitalhq.schemas.OperationKind
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import mu.KotlinLogging
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Captures operation invocation requests, and periodically writes counts
 * to the database.
 *
 * Note - this should never error, but should log if things go wrong.
 */
@Component
class OperationInvocationCountWriter(
   private val queueingOperationInvocationEventConsumer: ReactiveOperationInvocationEventConsumer,
   private val repository: OperationInvocationCountRepository,
   /**
    * Only set to true in tests. Otherwise, you could end up with an integer overflow exception
    */
   private val countEmittedEvents: Boolean = false
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val queuedEvents = LinkedBlockingQueue<OperationInvocationCountingEvent>(Int.MAX_VALUE)
   private val countedEvents = AtomicInteger(0)
   init {
      queueingOperationInvocationEventConsumer.operationInvokedEvents.subscribe {
         queuedEvents.offer(it)
         if (countEmittedEvents) {
            countedEvents.incrementAndGet()
         }
      }
   }

   // For testing purposes
   fun getCountedEvents() = countedEvents.get()

   @Scheduled(fixedRateString = "\${vyne.operation-count.write-frequency:PT5M}")
   fun writeNow() {
      val eventsByOperation = try {
         val drainTime = Instant.now()
         val events = mutableListOf<OperationInvocationCountingEvent>()
         queuedEvents.drainTo(events)
         if (events.isEmpty()) return

         val windowStart = events.minOf { it.timestamp }

         events
            .groupBy { it.operation }
            .map { (operation, calls) ->
               OperationInvocationCountWindow(
                  windowStart = windowStart,
                  windowEnd = drainTime,
                  operationName = operation.name,
                  operationKind = operation.operationKind,
                  count = calls.size
               )
            }
      } catch (e: Exception) {
         logger.warn(e) { "An error occurred gathering usage stats: ${e.message ?: e::class.simpleName}" }
         return
      }
      try {
         repository.saveAll(eventsByOperation)
      } catch (e: Exception) {
         logger.warn(e) { "Failed to capture operation counts: ${e.message}" }
      }

   }
}

@Entity(name = "operation_invocation_count")
data class OperationInvocationCountWindow(
   val windowStart: Instant,
   val windowEnd: Instant,
   val operationName: String,
   @Enumerated(EnumType.STRING)
   val operationKind: OperationKind,
   val count: Int,
   @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long = 0
) {
   fun merge(other: OperationInvocationCountWindow): OperationInvocationCountWindow {
      require(this.operationKind == other.operationKind) { "Expected both windows would have same OperationKind" }
      require(this.operationName == other.operationName) { "Expected both windows would have same Operation name" }
      return OperationInvocationCountWindow(
         windowStart = minOf(this.windowStart, other.windowStart),
         windowEnd = maxOf(this.windowEnd, other.windowEnd),
         operationName = this.operationName,
         operationKind = this.operationKind,
         count = this.count + other.count
      )

   }
}

interface OperationInvocationCountRepository : JpaRepository<OperationInvocationCountWindow, Long> {
   fun getAllByWindowStartBetween(start: Instant, end: Instant): List<OperationInvocationCountWindow>
}
