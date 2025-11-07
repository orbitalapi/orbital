package com.orbitalhq.query

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Operation
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow

/**
 * Simple lightweight way of providing operation calling capabilities into an accessor reader
 * for supporting OperationInvcoationExpressions.
 *
 * Will bubble back up to the TypedObjectFactory, then to the actual query context
 */
interface OperationInvokerContainer {
   suspend fun invokeOperation(
      service: Service,
      operation: RemoteOperation,
      providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()
   ): Flow<TypedInstance>
}
