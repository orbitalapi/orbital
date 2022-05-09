package io.vyne.connectors.jdbc

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.batch.OperationBatchingStrategy
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow

class JdbcBatchingInvoker : OperationBatchingStrategy {
   override fun canBatch(service: Service, operation: RemoteOperation, schema: Schema): Boolean {
      // TODO:
      // Under what conditions can we batch queries together?
      // I think we should be creating a batch for all queries with the
      // same set of filter criteria.
      // Remember that determining WHEN to batch isn't our responsibility
      // here, just "Is this query batchable"?
      TODO("Not yet implemented")
   }

   override fun invokeInBatch(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      TODO("Not yet implemented")
   }
}
