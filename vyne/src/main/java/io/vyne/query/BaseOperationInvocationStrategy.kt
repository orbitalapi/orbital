package io.vyne.query

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Type
import io.vyne.utils.log

abstract class BaseOperationInvocationStrategy(
   private val invocationService: OperationInvocationService
) {
   protected fun invokeOperations(operations: Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>>, context: QueryContext, target: Set<QuerySpecTypeNode>): QueryStrategyResult {
      val matchedNodes = operations.mapNotNull { (queryNode, operationToParameters) ->
         invokeOperation(queryNode, operationToParameters, context, target)
      }.toMap()

      return QueryStrategyResult(matchedNodes)
   }


   private fun invokeOperation(queryNode: QuerySpecTypeNode, operationToParameters: Map<RemoteOperation, Map<Parameter, TypedInstance>>, context: QueryContext, target: Set<QuerySpecTypeNode>): Pair<QuerySpecTypeNode, TypedInstance?>? {
      val operationsToInvoke = when {
         operationToParameters.size > 1 && queryNode.mode != QueryMode.GATHER -> {
            log().warn("Running in query mode ${queryNode.mode} and multiple candidate operations detected - ${operationToParameters.keys.joinToString { it.name }} - this isn't supported yet, will just pick the first one")
            listOf(operationToParameters.keys.first())
         }
         operationToParameters.isEmpty() -> emptyList()
         queryNode.mode == QueryMode.GATHER -> operationToParameters.keys.toList()
         else -> listOf(operationToParameters.keys.first())
      }

      if (operationsToInvoke.isEmpty()) {
         // Bail early
         return null
      }

      val serviceResults = operationsToInvoke.map { operation ->
         val parameters = operationToParameters.getValue(operation)
         val (service, _) = context.schema.remoteOperation(operation.qualifiedName)
         // Adding logging as seeing too many http calls.
         log().info("As part of search for ${target.joinToString { it.description }} operation ${operation.qualifiedName} will be invoked")
         val serviceResult = invocationService.invokeOperation(
            service,
            operation,
            context = context,
            preferredParams = emptySet(),
            providedParamValues = parameters.toList()
         )
         serviceResult
      }.flattenNestedTypedCollections(flattenedType = queryNode.type)


      val strategyResult = when {
         serviceResults.isEmpty() -> null
         serviceResults is TypedCollection -> serviceResults
         serviceResults.size == 1 -> serviceResults.first()
         else -> TypedCollection(queryNode.type, serviceResults) // Not sure this is a valid
      }
      return queryNode to strategyResult
   }
}

private fun List<TypedInstance>.flattenNestedTypedCollections(flattenedType: Type): List<TypedInstance> {
   return if (this.all { it is TypedCollection }) {
      val values = this.flatMap { (it as TypedCollection).value }
      TypedCollection(flattenedType, values)
   } else {
      this
   }
}
