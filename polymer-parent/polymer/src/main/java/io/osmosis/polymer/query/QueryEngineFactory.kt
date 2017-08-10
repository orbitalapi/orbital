package io.osmosis.polymer.query

import io.osmosis.polymer.SchemaPathResolver
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.graph.*
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvocationEvaluator
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvoker
import io.osmosis.polymer.schemas.Schema
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
                     RequiresParameterEvaluator(),
                     OperationInvocationEvaluator(invokers))
                  )
               )
            )
         )
      }
   }

   fun queryEngine(schema: Schema, pathResolver: SchemaPathResolver): QueryEngine {
      return DefaultQueryEngine(schema, strategies, pathResolver)
   }

   fun queryEngine(schema: Schema, models: Set<TypedInstance>, pathResolver: SchemaPathResolver): StatefulQueryEngine {
      return StatefulQueryEngine(models, queryEngine(schema, pathResolver))
   }
}
