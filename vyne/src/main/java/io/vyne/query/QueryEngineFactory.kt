package io.vyne.query

import io.vyne.FactSetMap
import io.vyne.VyneCacheConfiguration
import io.vyne.models.format.ModelFormatSpec
import io.vyne.query.connectors.OperationInvoker
import io.vyne.query.graph.EdgeNavigator
import io.vyne.query.graph.HipsterDiscoverGraphQueryStrategy
import io.vyne.query.graph.edges.ArrayMappingAttributeEvaluator
import io.vyne.query.graph.edges.AttributeOfEdgeEvaluator
import io.vyne.query.graph.edges.CanPopulateEdgeEvaluator
import io.vyne.query.graph.edges.EdgeEvaluator
import io.vyne.query.graph.edges.EnumSynonymEdgeEvaluator
import io.vyne.query.graph.edges.ExtendsTypeEdgeEvaluator
import io.vyne.query.graph.edges.HasAttributeEdgeEvaluator
import io.vyne.query.graph.edges.HasParamOfTypeEdgeEvaluator
import io.vyne.query.graph.edges.InstanceHasAttributeEdgeEvaluator
import io.vyne.query.graph.edges.IsInstanceOfEdgeEvaluator
import io.vyne.query.graph.edges.IsTypeOfEdgeEvaluator
import io.vyne.query.graph.edges.OperationParameterEdgeEvaluator
import io.vyne.query.graph.edges.QueryBuildingEvaluator
import io.vyne.query.graph.edges.RequiresParameterEdgeEvaluator
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationEvaluator
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.policyManager.DatasourceAwareOperationInvocationServiceDecorator
import io.vyne.query.policyManager.PolicyAwareOperationInvocationServiceDecorator
import io.vyne.query.projection.LocalProjectionProvider
import io.vyne.query.projection.ProjectionProvider
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
            emptyList(),
            emptyList(),
            LocalProjectionProvider())
      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun default(): QueryEngineFactory {
         return withOperationInvokers(
            VyneCacheConfiguration.default(),
            emptyList(),
            emptyList(),
            LocalProjectionProvider())
      }

//      fun jhipster(operationInvokers: List<OperationInvoker> = DefaultInvokers.invokers): JHipsterQueryEngineFactory {
//         return JHipsterQueryEngineFactory(edgeEvaluators(operationInvokers))
//      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun withOperationInvokers(
         vyneCacheConfiguration: VyneCacheConfiguration,
         formatSpecs:List<ModelFormatSpec> = emptyList(),
         vararg invokers: OperationInvoker): QueryEngineFactory {
         return withOperationInvokers(vyneCacheConfiguration, invokers.toList(), formatSpecs, projectionProvider = LocalProjectionProvider())
      }

      fun withOperationInvokers(
         vyneCacheConfiguration: VyneCacheConfiguration,
         invokers: List<OperationInvoker>,
         formatSpecs:List<ModelFormatSpec> = emptyList(),
         projectionProvider: ProjectionProvider = LocalProjectionProvider()): QueryEngineFactory {
         val invocationService = operationInvocationService(invokers)
         val opInvocationEvaluator = OperationInvocationEvaluator(invocationService)
         val edgeEvaluator = EdgeNavigator(edgeEvaluators(opInvocationEvaluator))
         val graphQueryStrategy = HipsterDiscoverGraphQueryStrategy(edgeEvaluator, vyneCacheConfiguration)

         return DefaultQueryEngineFactory(
            strategies = listOf(
//               CalculatedFieldScanStrategy(CalculatorRegistry()),
               ModelsScanStrategy(),
//               ProjectionHeuristicsQueryStrategy(opInvocationEvaluator, vyneCacheConfiguration.vyneGraphBuilderCache),
               //               PolicyAwareQueryStrategyDecorator(
               DirectServiceInvocationStrategy(invocationService),
               QueryOperationInvocationStrategy(invocationService),
               //
               //              ),
               graphQueryStrategy,
               ObjectBuilderStrategy()
               //,HipsterGatherGraphQueryStrategy()
            ),
            projectionProvider,
            operationInvocationService = invocationService,
            formatSpecs = formatSpecs
         )
      }

      private fun edgeEvaluators(operationInvocationEdgeEvaluator: EdgeEvaluator): List<EdgeEvaluator> {
         return listOf(
            RequiresParameterEdgeEvaluator(),
            AttributeOfEdgeEvaluator(),
            IsTypeOfEdgeEvaluator(),
            HasParamOfTypeEdgeEvaluator(),
            IsInstanceOfEdgeEvaluator(),
            InstanceHasAttributeEdgeEvaluator(),
            OperationParameterEdgeEvaluator(),
            HasAttributeEdgeEvaluator(),
            CanPopulateEdgeEvaluator(),
            ExtendsTypeEdgeEvaluator(),
            EnumSynonymEdgeEvaluator(),
            QueryBuildingEvaluator(),
            ArrayMappingAttributeEvaluator(),
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

class DefaultQueryEngineFactory(
   private val strategies: List<QueryStrategy>,
   private val projectionProvider: ProjectionProvider,
   private val operationInvocationService: OperationInvocationService,
   private val formatSpecs:List<ModelFormatSpec> = emptyList()
) : QueryEngineFactory {

   override fun queryEngine(schema: Schema): QueryEngine {
      return queryEngine(schema, FactSetMap.create())
   }

   override fun queryEngine(schema: Schema, models: FactSetMap): StatefulQueryEngine {
      return StatefulQueryEngine(models, schema, strategies,projectionProvider = projectionProvider, operationInvocationService = operationInvocationService, formatSpecs = formatSpecs)
   }
}
