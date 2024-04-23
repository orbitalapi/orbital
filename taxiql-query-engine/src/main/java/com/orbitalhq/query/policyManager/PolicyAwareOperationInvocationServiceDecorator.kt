package com.orbitalhq.query.policyManager

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lang.taxi.policies.PolicyOperationScope

class PolicyAwareOperationInvocationServiceDecorator(private val operationService: OperationInvocationService, private val evaluator: PolicyEvaluator = PolicyEvaluator()) : OperationInvocationService {
   override suspend fun invokeOperation(service: Service, operation: RemoteOperation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>>): Flow<TypedInstance> {
      // For now, treating everything as external.
      // Need to update the query manager to differentiate between external
      // TODO: Get these from the operation (operationType) and query engine (scope)
      val executionScope = ExecutionScope(operationScope = operation.operationType, policyOperationScope = PolicyOperationScope.EXTERNAL)


      // We invoke the operation regardless, as current thinking is that we're
      // policing data, not service access.
      // A future optimization would be to determine if the data won't be read, and
      // then to prevent calling the service entirely.
      // However, it's difficult to be able to express that policy, as we won't
      // have access to the data yet, so rules aren't easily expressable.
      // Service / operation level security is likely best handled as a specific service / operation
      // level concern, outside of Vyne on the service itself.
      val result = operationService.invokeOperation(service, operation, preferredParams, context, providedParamValues)
      return process(result, context, executionScope)

   }


   private fun process(
      value: Flow<TypedInstance>,
      context: QueryContext,
      executionScope: ExecutionScope
   ): Flow<TypedInstance> {
      return value.map { t ->
         applyPolicyInstruction(t, context, executionScope)
      }
   }


   private fun applyPolicyInstruction(
      value: TypedInstance,
      context: QueryContext,
      executionScope: ExecutionScope
   ): TypedInstance {
      return evaluator.evaluate(value, context, executionScope)
   }
}
