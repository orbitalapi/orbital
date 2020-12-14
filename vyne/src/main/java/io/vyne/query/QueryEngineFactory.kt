package io.vyne.query

import io.vyne.FactSetMap
import io.vyne.VyneCacheConfiguration
import io.vyne.VyneGraphBuilderCacheSettings
import io.vyne.formulas.CalculatorRegistry
import io.vyne.query.graph.AttributeOfEdgeEvaluator
import io.vyne.query.graph.AttributeOfEvaluator
import io.vyne.query.graph.CanPopulateEdgeEvaluator
import io.vyne.query.graph.EdgeEvaluator
import io.vyne.query.graph.ExtendsTypeEdgeEvaluator
import io.vyne.query.graph.HasAttributeEdgeEvaluator
import io.vyne.query.graph.HasAttributeEvaluator
import io.vyne.query.graph.HasParamOfTypeEdgeEvaluator
import io.vyne.query.graph.InstanceHasAttributeEdgeEvaluator
import io.vyne.query.graph.IsInstanceOfEdgeEvaluator
import io.vyne.query.graph.IsTypeOfEdgeEvaluator
import io.vyne.query.graph.IsTypeOfEvaluator
import io.vyne.query.graph.LinkEvaluator
import io.vyne.query.graph.OperationParameterEdgeEvaluator
import io.vyne.query.graph.OperationParameterEvaluator
import io.vyne.query.graph.RequiresParameterEdgeEvaluator
import io.vyne.query.graph.RequiresParameterEvaluator
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationEvaluator
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.query.planner.ProjectionHeuristicsQueryStrategy
import io.vyne.query.policyManager.DatasourceAwareOperationInvocationServiceDecorator
import io.vyne.query.policyManager.PolicyAwareOperationInvocationServiceDecorator
import io.vyne.schemas.Schema


interface QueryEngineFactory {
   fun queryEngine(schema: Schema, models: FactSetMap): StatefulQueryEngine
   fun queryEngine(schema: Schema): QueryEngine

//   val pathResolver: SchemaPathResolver

   companion object {
      // Useful for testing
      fun noQueryEngine(): QueryEngineFactory {
         return withOperationInvokers(
            VyneCacheConfiguration.default(),
            emptyList())
      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun default(): QueryEngineFactory {
         return withOperationInvokers(
            VyneCacheConfiguration.default(),
            emptyList())
      }

//      fun jhipster(operationInvokers: List<OperationInvoker> = DefaultInvokers.invokers): JHipsterQueryEngineFactory {
//         return JHipsterQueryEngineFactory(edgeEvaluators(operationInvokers))
//      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun withOperationInvokers(vyneCacheConfiguration: VyneCacheConfiguration, vararg invokers: OperationInvoker): QueryEngineFactory {
         return withOperationInvokers(vyneCacheConfiguration, invokers.toList())
      }

      fun withOperationInvokers(vyneCacheConfiguration: VyneCacheConfiguration, invokers: List<OperationInvoker>): QueryEngineFactory {
         val invocationService = operationInvocationService(invokers)
         val opInvocationEvaluator = OperationInvocationEvaluator(invocationService)
         val edgeEvaluator = EdgeNavigator(edgeEvaluators(opInvocationEvaluator))
         val graphQueryStrategy = HipsterDiscoverGraphQueryStrategy(edgeEvaluator, vyneCacheConfiguration)

         return DefaultQueryEngineFactory(
            strategies = listOf(
               CalculatedFieldScanStrategy(CalculatorRegistry()),
               ModelsScanStrategy(),
               ProjectionHeuristicsQueryStrategy(opInvocationEvaluator, vyneCacheConfiguration.vyneGraphBuilderCache),
               //               PolicyAwareQueryStrategyDecorator(
               DirectServiceInvocationStrategy(invocationService),
               QueryOperationInvocationStrategy(invocationService),
               //
               //              ),
               graphQueryStrategy,
               HipsterGatherGraphQueryStrategy())
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
         return DatasourceAwareOperationInvocationServiceDecorator(PolicyAwareOperationInvocationServiceDecorator(
            DefaultOperationInvocationService(invokers)
         ))
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
