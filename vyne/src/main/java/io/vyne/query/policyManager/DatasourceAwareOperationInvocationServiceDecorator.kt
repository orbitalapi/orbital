package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow

class DatasourceAwareOperationInvocationServiceDecorator(private val operationService: OperationInvocationService): OperationInvocationService {
   override suspend fun invokeOperation(service: Service, operation: RemoteOperation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>>): Flow<TypedInstance> {
      val result = operationService.invokeOperation(service, operation, preferredParams, context, providedParamValues)
      context.onServiceInvoked(service)
      return result
   }
}
