package com.orbitalhq.query.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.EmptyExchangeData
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class OperationCachingBehaviour {
   NO_CACHE,
   CACHING_PERMITTED
}

interface OperationInvocationPlanner {
   fun canSupport(service: Service, operation: RemoteOperation): Boolean
   /**
    * Returns a plan of what this operation will do - but does not invoke it.
    * Used when generating query plan diagrams.
    *
    * If not implemented, will return a default RemoteCall lacking any specifics
    */
   fun plan(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): RemoteCall {
      return RemoteCall(
         service = OperationNames.serviceName(operation.qualifiedName).fqn(),
         address = "Not available",
         operation = operation.name,
         responseTypeName = operation.returnType.qualifiedName,
         method = "Not available",
         requestBody = "Not available",
         resultCode = 200,
         durationMs = 0,
         response = "Not available",
         timestamp = Instant.now(),
         responseMessageType = ResponseMessageType.FULL,
         exchange = EmptyExchangeData

      )
   }
}
interface OperationInvoker: OperationInvocationPlanner {
   fun getCachingBehaviour(service: Service, operation: RemoteOperation): OperationCachingBehaviour {
      return OperationCachingBehaviour.CACHING_PERMITTED
   }



   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<TypedInstance>
}
