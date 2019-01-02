package io.vyne.query.policyManager

import io.vyne.models.*
import io.vyne.query.QueryContext
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Operation
import io.vyne.schemas.Service
import lang.taxi.policies.OperationScope

class PolicyAwareOperationInvocationServiceDecorator(private val operationService: OperationInvocationService, private val evaluator: PolicyEvaluator = PolicyEvaluator()) : OperationInvocationService {
   override fun invokeOperation(service: Service, operation: Operation, preferredParams: Set<TypedInstance>, context: QueryContext): TypedInstance {
      // For now, treating everything as external.
      // Need to update the query manager to differentiate between external
      // TODO: Get these from the operation (operationType) and query engine (scope)
      val executionScope = ExecutionScope(operationType = operation.operationType, operationScope = OperationScope.EXTERNAL)


      // We invoke the operation regardless, as current thinking is that we're
      // policing data, not service access.
      // A future optimization would be to determine if the data won't be read, and
      // then to prevent calling the service entirely.
      // However, it's difficult to be able to express that policy, as we won't
      // have access to the data yet, so rules aren't easily expressable.
      // Service / operation level security is likely best handled as a specific service / operation
      // level concern, outside of Vyne on the service itself.
      val result = operationService.invokeOperation(service, operation, preferredParams, context)

      val processed = process(result, context, executionScope)
      return processed
   }

   private fun process(value: TypedInstance, context: QueryContext, executionScope: ExecutionScope): TypedInstance {
      val processedValue = applyPolicyInstruction(value, context, executionScope)

      return when (processedValue) {
         is TypedNull -> processedValue // Nothing more to do.
         is TypedValue -> processedValue // Can't recurse any further
         is TypedObject -> processTypedObject(processedValue, context, executionScope)
         is TypedCollection -> processCollection(processedValue, context, executionScope)
         else -> TODO()
      }
   }

   private fun processCollection(collection: TypedCollection, context: QueryContext, executionScope: ExecutionScope): TypedInstance {
      val processedValues = collection.map { process(it, context, executionScope) }
         .filter { it !is TypedNull }
      return TypedCollection(collection.type, processedValues)
   }

   private fun processTypedObject(typedObject: TypedObject, context: QueryContext, executionScope: ExecutionScope): TypedInstance {
      val processedAttributes = typedObject.value.map { (propertyName, value) ->
         propertyName to process(value, context, executionScope)
      }.toMap()
      return TypedObject(typedObject.type, processedAttributes)
   }

   private fun applyPolicyInstruction(value: TypedInstance, context: QueryContext, executionScope: ExecutionScope): TypedInstance {
      val instruction = evaluator.evaluate(value, context, executionScope)
      val processed = InstructionExecutors.get(instruction).execute(instruction, value)
      return processed
   }

}
