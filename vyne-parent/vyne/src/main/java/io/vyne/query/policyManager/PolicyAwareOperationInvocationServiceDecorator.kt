package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Operation
import io.vyne.schemas.Service
import lang.taxi.policies.OperationScope
import lang.taxi.policies.PolicyScope

class PolicyAwareOperationInvocationServiceDecorator(private val operationService: OperationInvocationService, val evaluator: PolicyEvaluator = PolicyEvaluator()) : OperationInvocationService {
   override fun invokeOperation(service: Service, operation: Operation, preferredParams: Set<TypedInstance>, context: QueryContext): TypedInstance {
      // For now, treating everything as external.
      // Need to update the query manager to differentiate between external
      // TODO: Get these from the operation (operationType) and query engine (scope)
      val executionScope = ExecutionScope(operationType = operation.operationType, operationScope = OperationScope.EXTERNAL)
      val instruction = evaluator.evaluate(operation.returnType, context, executionScope)

      return operationService.invokeOperation(service, operation, preferredParams, context)
   }

}
