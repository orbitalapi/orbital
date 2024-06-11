package com.orbitalhq.query.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow

/**
 * Ultimately responsible for counting calls to operation invokers.
 * Part of our licensing strategy.
 */
interface OperationInvocationEventConsumer {
   /**
    * Called every time an operation is invoked.
    * Intended for counting.
    *
    * Must not block. Must not throw an exception
    */
   fun operationInvoked(operation: RemoteOperation)
}

class CountingOperationInvokerDecorator(
   private val invoker: OperationInvoker,
   private val operationInvocationEventConsumer: OperationInvocationEventConsumer
) : OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return invoker.canSupport(service, operation)
   }

   companion object {
      fun decorateAll(
         invokers: List<OperationInvoker>,
         operationInvocationEventConsumer: OperationInvocationEventConsumer
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
      operationInvocationEventConsumer.operationInvoked(operation)
      return invoker.invoke(service, operation, parameters, eventDispatcher, queryId, queryOptions)
   }

}
