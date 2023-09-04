package com.orbitalhq.query.policyManager

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DatasourceAwareOperationInvocationServiceDecorator(private val operationService: OperationInvocationService): OperationInvocationService {
   override suspend fun invokeOperation(service: Service, operation: RemoteOperation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>>): Flow<TypedInstance> = withContext(
      Dispatchers.Default) {
      val result = operationService.invokeOperation(service, operation, preferredParams, context, providedParamValues)
      context.onServiceInvoked(service)
      result
   }
}
