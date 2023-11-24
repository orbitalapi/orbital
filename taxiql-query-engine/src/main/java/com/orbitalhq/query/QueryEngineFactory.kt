package com.orbitalhq.query

import com.orbitalhq.FactSetMap
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.graph.EdgeNavigator
import com.orbitalhq.query.graph.HipsterDiscoverGraphQueryStrategy
import com.orbitalhq.query.graph.edges.ArrayMappingAttributeEvaluator
import com.orbitalhq.query.graph.edges.AttributeOfEdgeEvaluator
import com.orbitalhq.query.graph.edges.CanPopulateEdgeEvaluator
import com.orbitalhq.query.graph.edges.EdgeEvaluator
import com.orbitalhq.query.graph.edges.EnumSynonymEdgeEvaluator
import com.orbitalhq.query.graph.edges.ExtendsTypeEdgeEvaluator
import com.orbitalhq.query.graph.edges.HasAttributeEdgeEvaluator
import com.orbitalhq.query.graph.edges.HasParamOfTypeEdgeEvaluator
import com.orbitalhq.query.graph.edges.InstanceHasAttributeEdgeEvaluator
import com.orbitalhq.query.graph.edges.IsInstanceOfEdgeEvaluator
import com.orbitalhq.query.graph.edges.IsTypeOfEdgeEvaluator
import com.orbitalhq.query.graph.edges.OperationParameterEdgeEvaluator
import com.orbitalhq.query.graph.edges.QueryBuildingEvaluator
import com.orbitalhq.query.graph.edges.RequiresParameterEdgeEvaluator
import com.orbitalhq.query.graph.operationInvocation.DefaultOperationInvocationService
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationEvaluator
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.query.policyManager.DatasourceAwareOperationInvocationServiceDecorator
import com.orbitalhq.query.policyManager.PolicyAwareOperationInvocationServiceDecorator
import com.orbitalhq.query.projection.LocalProjectionProvider
import com.orbitalhq.query.projection.ProjectionProvider
import com.orbitalhq.schemas.Schema


interface QueryEngineFactory {
   fun queryEngine(schema: Schema, models: FactSetMap, metricsTags: Map<String,String> = emptyMap()): StatefulQueryEngine
   fun queryEngine(schema: Schema, metricsTags: Map<String,String> = emptyMap()): QueryEngine

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
         projectionProvider: ProjectionProvider = LocalProjectionProvider(),
         queryMetricsReporter: QueryMetricsReporter = NoOpMetricsReporter): QueryEngineFactory {
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
            formatSpecs = formatSpecs,
            metricsReporter = queryMetricsReporter
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
   private val formatSpecs:List<ModelFormatSpec> = emptyList(),
   private val metricsReporter: QueryMetricsReporter = NoOpMetricsReporter
) : QueryEngineFactory {

   override fun queryEngine(schema: Schema, metricsTags: Map<String, String>): QueryEngine {
      return queryEngine(schema, FactSetMap.create())
   }

   override fun queryEngine(schema: Schema, models: FactSetMap, metricsTags: Map<String,String>): StatefulQueryEngine {
      return StatefulQueryEngine(models, schema, strategies,projectionProvider = projectionProvider, operationInvocationService = operationInvocationService, formatSpecs = formatSpecs,
         metricsReporter = metricsReporter)
   }
}
