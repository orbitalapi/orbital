package io.vyne.query

import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.utils.log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
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


      val queryStrategyResult = QueryStrategyResult(matchedNodes)
      return observeAndNotifyOperationResults(queryStrategyResult, context)
   }

   /**
    * If the query strategy was successful, add a listener for each result as it comes out of the
    * operation result flow, and notify the query context of the successful result.
    * This enables visualtions / analytics, not for tracking actual results.
    */
   private fun observeAndNotifyOperationResults(
      queryStrategyResult: QueryStrategyResult,
      context: QueryContext
   ): QueryStrategyResult {
      // MP: 24-Oct-21.
      // In testing Sankey chart generation, I noticed that we're not consistently observing the results of service
      // invocation between directService (where there are no input params) and graphService (where there are input params)
      // invocation.  The graphService caches results, which I've chosen not to do here in order to minimize introducing
      // side effects at this stage.  However, we may wish to modify this to cache these operations too.
      // Considerations:
      // * Graph invocation calls .toList() on the result before caching, wheras this does not.
      // The assumption there is that graph invocation services are not streaming, and only return single results.
      // However, that may not always be true.
      // Direct service invocation DOES hit streaming services (currently), so we need to tread carefully here, as there
      // are sometimes infinite results.
      // MP: 1-Mar-22 : I think the above is incorrect.
      // We cache in a different way here -
      // invokeOperations calls to the invocationService, which has been decorated
      // with a CacheAwareOperationInvokerDecorator, which handles the caching.
      return if (queryStrategyResult.hasMatchesNodes()) {
         val observedFlow = queryStrategyResult.matchedNodes.onEach { instance ->
            val instanceSource = instance.source
            if (instanceSource is OperationResult) {
               context.notifyOperationResult(instanceSource)
            }
         }
         queryStrategyResult.copy(nullableMatchedNodes = observedFlow)
      } else {
         queryStrategyResult
      }
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
         log().info("[${context.queryId}] As part of search for ${target.joinToString { it.description }} operation ${operation.qualifiedName} will be invoked.")

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


