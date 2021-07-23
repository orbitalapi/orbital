package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.utils.log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class BaseOperationInvocationStrategy(
   private val invocationService: OperationInvocationService
) {
   @ExperimentalCoroutinesApi
   protected suspend fun invokeOperations(
      operations: Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>>,
      context: QueryContext,
      target: Set<QuerySpecTypeNode>
   ): QueryStrategyResult {
      if (operations.isEmpty()) {
         return QueryStrategyResult.searchFailed()
      }

      val matchedNodes =
         operations.mapNotNull { (queryNode, operationToParameters) ->
            invokeOperation(queryNode, operationToParameters, context, target)
         }.merge().map {
//            logger.info { "BaseOperationInvocationStrategy map received item" }
            it.second
         }

      return QueryStrategyResult(matchedNodes)
   }


   @ExperimentalCoroutinesApi
   private suspend fun invokeOperation(
      queryNode: QuerySpecTypeNode,
      operationToParameters: Map<RemoteOperation, Map<Parameter, TypedInstance>>,
      context: QueryContext,
      target: Set<QuerySpecTypeNode>
   ): Flow<Pair<QuerySpecTypeNode, TypedInstance>>? {
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
      } else {
      }


      return operationsToInvoke.map { operation ->
         val parameters = operationToParameters.getValue(operation)
         val (service, _) = context.schema.remoteOperation(operation.qualifiedName)
         // Adding logging as seeing too many http calls.
         log().info("As part of search for ${target.joinToString { it.description }} operation ${operation.qualifiedName} will be invoked for queryId ${context.queryId}")

         invocationService.invokeOperation(
            service,
            operation,
            context = context,
            preferredParams = emptySet(),
            providedParamValues = parameters.toList()
         )

         // NOTE - merge() will take signals from flows as they arrive !! order is not maintained !!
      }.merge().map {
//         logger.info { "BaseOperationInvocationStrategy merge saw item" }
         queryNode to it
      }

   }

}


