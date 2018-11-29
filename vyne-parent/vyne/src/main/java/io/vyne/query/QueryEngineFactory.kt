package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.query.graph.*
import io.vyne.query.graph.operationInvocation.OperationInvocationEvaluator
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.query.graph.LinkEvaluator
import io.vyne.schemas.Schema


interface QueryEngineFactory {
   fun queryEngine(schema: Schema, models: Set<TypedInstance>): StatefulQueryEngine
   fun queryEngine(schema: Schema): QueryEngine

//   val pathResolver: SchemaPathResolver

   companion object {
      // Useful for testing
      fun noQueryEngine(): QueryEngineFactory {
         return withOperationInvokers(emptyList())
      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun default(): QueryEngineFactory {
         return withOperationInvokers(emptyList())
      }

//      fun jhipster(operationInvokers: List<OperationInvoker> = DefaultInvokers.invokers): JHipsterQueryEngineFactory {
//         return JHipsterQueryEngineFactory(edgeEvaluators(operationInvokers))
//      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun withOperationInvokers(vararg invokers: OperationInvoker): QueryEngineFactory {
         return withOperationInvokers(invokers.toList())
      }

      fun withOperationInvokers(invokers: List<OperationInvoker>): QueryEngineFactory {
         val edgeEvaluator = EdgeNavigator(edgeEvaluators(invokers))
         val graphQueryStrategy = HipsterDiscoverGraphQueryStrategy(
            edgeEvaluator
         )
         return DefaultQueryEngineFactory(
            strategies = listOf(
               ModelsScanStrategy(),
               DirectServiceInvocationStrategy(invokers),
               graphQueryStrategy,
               HipsterGatherGraphQueryStrategy(graphQueryStrategy)
            )
         )
      }

      private fun linkEvaluators(invokers: List<OperationInvoker>): List<LinkEvaluator> {
         return listOf(AttributeOfEvaluator(),
            HasAttributeEvaluator(),
            IsTypeOfEvaluator(),
//            HasParamOfTypeEvaluator(),
            OperationParameterEvaluator(),
            RequiresParameterEvaluator(),
            OperationInvocationEvaluator(invokers))
      }

      fun edgeEvaluators(invokers: List<OperationInvoker>): List<EdgeEvaluator> {
         return listOf(RequiresParameterEdgeEvaluator(),
            AttributeOfEdgeEvaluator(),
            IsTypeOfEdgeEvaluator(),
            HasParamOfTypeEdgeEvaluator(),
            IsInstanceOfEdgeEvaluator(),
            InstanceHasAttributeEdgeEvaluator(),
            OperationParameterEdgeEvaluator(),
            HasAttributeEdgeEvaluator(),
            CanPopulateEdgeEvaluator(),
            OperationInvocationEvaluator(invokers)
         )
      }
   }

}

class DefaultQueryEngineFactory(private val strategies: List<QueryStrategy>) : QueryEngineFactory {

   override fun queryEngine(schema: Schema): QueryEngine {
      return DefaultQueryEngine(schema, strategies)
   }

   override fun queryEngine(schema: Schema, models: Set<TypedInstance>): StatefulQueryEngine {
      return StatefulQueryEngine(models, queryEngine(schema))
   }
}
