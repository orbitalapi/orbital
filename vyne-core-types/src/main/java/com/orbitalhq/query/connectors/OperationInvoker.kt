package com.orbitalhq.query.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow

enum class OperationCachingBehaviour {
   NO_CACHE,
   CACHING_PERMITTED
}
interface OperationInvoker {
   fun canSupport(service: Service, operation: RemoteOperation): Boolean

   fun getCachingBehaviour(service: Service, operation: RemoteOperation): OperationCachingBehaviour {
      return OperationCachingBehaviour.CACHING_PERMITTED
   }

   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance>
}
