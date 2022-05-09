package io.vyne.query.connectors.batch

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow

/**
 * Checks to see if an operation can be
 * converted into a batched operation.
 *
 * If so, is responsible for receving the
 * unbatched invocation request, batching it,
 * and unbatching the result flow, such that
 * the requestor was unaware of the batching
 * that took place.
 */
interface OperationBatchingStrategy {
   fun canBatch(
      service: Service,
      operation: RemoteOperation,
      schema: Schema
   ): Boolean

   /**
    * Appends the request to a pending batch
    * (or creates a new pending batch if one doesn't
    * exist).
    *
    * Will eventually resolve with the result
    * of the call, having unwrapped back to the
    * single value.
    */
   fun invokeInBatch(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String? = null
   ): Flow<TypedInstance>
}
