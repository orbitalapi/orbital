package io.osmosis.polymer.query

import io.osmosis.polymer.query.graph.*
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvocationEvaluator
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvoker
import org.springframework.stereotype.Component

@Component
class QueryEngineFactory(private val strategies: List<QueryStrategy>) {

   companion object {
      // Useful for testing
      fun noQueryEngine(): QueryEngineFactory {
         return QueryEngineFactory(emptyList())
      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun default(): QueryEngineFactory {
         return withOperationInvokers(emptyList())
      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun withOperationInvokers(vararg invokers: OperationInvoker): QueryEngineFactory {
         return withOperationInvokers(invokers.toList())
      }

      fun withOperationInvokers(invokers: List<OperationInvoker>): QueryEngineFactory {
         return QueryEngineFactory(
            strategies = listOf(ModelsScanStrategy(),
               GraphResolutionStrategy(
                  PathEvaluator(linkEvaluators = listOf(AttributeOfEvaluator(),
                     HasAttributeEvaluator(),
                     IsTypeOfEvaluator(),
                     OperationParameterEvaluator(),
                     OperationInvocationEvaluator(invokers))
                  )
               )
            )
         )
      }
   }

   fun queryEngine(context: QueryContext): QueryEngine {
      return QueryEngine(context, strategies)
   }
}
