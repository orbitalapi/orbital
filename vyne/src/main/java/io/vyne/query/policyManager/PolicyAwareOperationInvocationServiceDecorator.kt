package io.vyne.query.policyManager

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.query.QueryContext
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import lang.taxi.policies.OperationScope

class PolicyAwareOperationInvocationServiceDecorator(private val operationService: OperationInvocationService, private val evaluator: PolicyEvaluator = PolicyEvaluator()) : OperationInvocationService {
   override suspend fun invokeOperation(service: Service, operation: RemoteOperation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>>): Flow<TypedInstance> {
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
      val result = operationService.invokeOperation(service, operation, preferredParams, context, providedParamValues)
      return process(result, context, executionScope)

   }


   private suspend fun process(value: Flow<TypedInstance>, context: QueryContext, executionScope: ExecutionScope): Flow<TypedInstance> {
      return value.map {t ->
         applyPolicyInstruction(t, context, executionScope)
      }
   }


   /*
   private suspend fun processCollection(collection: TypedCollection, context: QueryContext, executionScope: ExecutionScope): Flow<TypedInstance> {
      val processedValues = collection.map { process(it, context, executionScope) }
         .filter { it !is TypedNull }
      return TypedCollection(collection.type, processedValues)
   }

   private suspend fun processTypedObject(typedObject: TypedObject, context: QueryContext, executionScope: ExecutionScope): Flow<TypedInstance> {
      val processedAttributes = typedObject.value.map { (propertyName, value) ->
         propertyName to process(value, context, executionScope)
      }.toMap()
      return TypedObject(typedObject.type, processedAttributes, typedObject.source)
   }
*/
   private suspend fun applyPolicyInstruction(value: TypedInstance, context: QueryContext, executionScope: ExecutionScope): TypedInstance {
      val instruction = evaluator.evaluate(value, context, executionScope)
      val processed = InstructionExecutors.get(instruction).execute(instruction, value)
      return processed
   }

}
