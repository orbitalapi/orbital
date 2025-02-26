package com.orbitalhq.query.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import java.time.Instant

/**
 * Part of our licensing strategy.
 * Ultimately responsible for counting calls to operation invokers.
 *
 */
interface OperationInvocationCountingEventConsumer {
   /**
    * Called every time an operation is invoked.
    * Intended for counting.
    *
    * Must not block. Must not throw an exception.
    *
    */
   fun operationInvoked(event: OperationInvocationCountingEvent)

   /**
    * Tracks that an operation emitted a result. Intended for counting purposes, so the
    * result is not provided.
    */
   fun operationEmittedResult(event: OperationInvocationCountingEvent)

   /**
    * Tracks that an operation emitted an error. Intended for counting purposes, so the
    * result is not provided.
    */
   fun operationThrewError(event: OperationInvocationCountingEvent)
}

/**
 * An event that an operation was invoked.
 * Intended for counting, so is intentionally low-fidelity.
 * ie., doesn't contain error details, or response bodies, etc.
 */
data class OperationInvocationCountingEvent(
   val operation: RemoteOperation,
   val queryId: String,

   // The plan was that these would be passed upstream, allowing
   // us to attribute metrics to a specific query.
   // But, we haven't been able to successfully wire this through,
   // so no-one actually sets these at the moment.
   // The value is published out to Micrometer, but no-ones actually
   // populating currently.
   val metricTags: MetricTags = MetricTags.NONE,
   val timestamp: Instant = Instant.now()
)

object NoOperationInvocationEventConsumer : OperationInvocationCountingEventConsumer {
   override fun operationInvoked(event: OperationInvocationCountingEvent) {
   }

   override fun operationEmittedResult(event: OperationInvocationCountingEvent) {
   }

   override fun operationThrewError(event: OperationInvocationCountingEvent) {
   }

}

/**
 * Licensing-focussed invocation counting
 */
class CountingOperationInvokerDecorator(
   private val invoker: OperationInvoker,
   // Design choice:
   // Intentionally keeping this to a single-consumer, rather than multiple.
   // We want to ensure these stay fast, as they're on they "hot path".
   // So, the decision was to use a single consumer, which has reactive Sink/Flux,
   // which allows processing to happen off the hot-path, and back pressure to
   // let us drop signals, if needed.
   private val operationInvocationEventConsumer: OperationInvocationCountingEventConsumer
) : OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return invoker.canSupport(service, operation)
   }

   companion object {
      fun decorateAll(
         invokers: List<OperationInvoker>,
         operationInvocationEventConsumer: OperationInvocationCountingEventConsumer
      ): List<OperationInvoker> {
         return invokers.map {
            CountingOperationInvokerDecorator(it, operationInvocationEventConsumer)
         }
      }
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<TypedInstance> {
      val event = OperationInvocationCountingEvent(operation, queryId, MetricTags.NONE)
      operationInvocationEventConsumer.operationInvoked(event)
      return invoker.invoke(service, operation, parameters, eventDispatcher, queryId, queryOptions)
         .onEach {
            operationInvocationEventConsumer.operationEmittedResult(event)
         }
         .catch { e ->
            operationInvocationEventConsumer.operationThrewError(event)
            throw e
         }
   }

}
