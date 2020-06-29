package io.vyne.query

import io.vyne.FactSetMap
import io.vyne.query.graph.*
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationEvaluator
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.query.planner.ProjectionHeuristicsQueryStrategy
import io.vyne.query.policyManager.PolicyAwareOperationInvocationServiceDecorator
import io.vyne.schemas.Schema


interface QueryEngineFactory {
   fun queryEngine(schema: Schema, models: FactSetMap): StatefulQueryEngine
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
         val opInvocationEvaluator = OperationInvocationEvaluator(operationInvocationService(invokers))
         val edgeEvaluator = EdgeNavigator(edgeEvaluators(opInvocationEvaluator))
         val graphQueryStrategy = HipsterDiscoverGraphQueryStrategy(
            edgeEvaluator
         )

         return DefaultQueryEngineFactory(
            strategies = listOf(
               ModelsScanStrategy(),
               ProjectionHeuristicsQueryStrategy(opInvocationEvaluator),
               //               PolicyAwareQueryStrategyDecorator(
               DirectServiceInvocationStrategy(operationInvocationService(invokers)),
               //
               //              ),
               graphQueryStrategy,
               HipsterGatherGraphQueryStrategy(graphQueryStrategy))
         )
      }

      private fun linkEvaluators(invokers: List<OperationInvoker>): List<LinkEvaluator> {
         return listOf(AttributeOfEvaluator(),
            HasAttributeEvaluator(),
            IsTypeOfEvaluator(),
//            HasParamOfTypeEvaluator(),
            OperationParameterEvaluator(),
            RequiresParameterEvaluator(),
            OperationInvocationEvaluator(operationInvocationService(invokers)))
      }

      private fun edgeEvaluators(operationInvocationEdgeEvaluator: EdgeEvaluator): List<EdgeEvaluator> {
         return listOf(RequiresParameterEdgeEvaluator(),
            AttributeOfEdgeEvaluator(),
            IsTypeOfEdgeEvaluator(),
            HasParamOfTypeEdgeEvaluator(),
            IsInstanceOfEdgeEvaluator(),
            InstanceHasAttributeEdgeEvaluator(),
            OperationParameterEdgeEvaluator(),
            HasAttributeEdgeEvaluator(),
            CanPopulateEdgeEvaluator(),
            ExtendsTypeEdgeEvaluator(),
            operationInvocationEdgeEvaluator
         )
      }

      private fun operationInvocationService(invokers: List<OperationInvoker>): OperationInvocationService {
         return PolicyAwareOperationInvocationServiceDecorator(
            DefaultOperationInvocationService(invokers)
         )
      }

   }


}

class DefaultQueryEngineFactory(private val strategies: List<QueryStrategy>) : QueryEngineFactory {

   override fun queryEngine(schema: Schema): QueryEngine {
      return queryEngine(schema, FactSetMap.create())
   }

   override fun queryEngine(schema: Schema, models: FactSetMap): StatefulQueryEngine {
      return StatefulQueryEngine(models, schema, strategies)
   }
}
