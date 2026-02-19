package com.orbitalhq.query

import com.orbitalhq.FactSetMap
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.graph.EdgeNavigator
import com.orbitalhq.query.graph.GraphSearchQueryStrategy
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
import com.orbitalhq.query.graph.edges.NoArgServiceEdgeEvaluator
import com.orbitalhq.query.graph.edges.IsTypeOfEdgeEvaluator
import com.orbitalhq.query.graph.edges.OperationParameterEdgeEvaluator
import com.orbitalhq.query.graph.edges.QueryBuildingEvaluator
import com.orbitalhq.query.graph.edges.RequiresParameterEdgeEvaluator
import com.orbitalhq.query.graph.operationInvocation.DefaultOperationInvocationService
import com.orbitalhq.query.graph.edges.ExpressionEvaluator
import com.orbitalhq.query.graph.edges.StartFactEdgeEvaluator
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationEvaluator
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.query.policyManager.DatasourceAwareOperationInvocationServiceDecorator
import com.orbitalhq.query.policyManager.PolicyAwareOperationInvocationServiceDecorator
import com.orbitalhq.query.projection.LocalProjectionProvider
import com.orbitalhq.query.projection.ProjectionProvider
import com.orbitalhq.query.streams.StreamMergingQueryStrategy
import com.orbitalhq.schemas.Schema
import kotlin.reflect.KClass

typealias QueryStrategyFilter = (QueryStrategy)-> Boolean

interface QueryEngineFactory {
   fun queryEngine(
      schema: Schema,
      models: FactSetMap,
      metricsTags: Map<String, String> = emptyMap()
   ): StatefulQueryEngine

   fun queryEngine(schema: Schema, metricsTags: Map<String, String> = emptyMap()): QueryEngine

//   val pathResolver: SchemaPathResolver

   companion object {
      // Useful for testing
      fun noQueryEngine(): QueryEngineFactory {
         return withOperationInvokers(
            VyneCacheConfiguration.default(),
            emptyList(),
            emptyList(),
            LocalProjectionProvider()
         )
      }

      val DEFAULT_QUERY_STRATEGY_FILTER:(QueryStrategy) -> Boolean = { true }

      /**
       * Allows removing specific strategies from the query engine
       * Only useful for testing
       */
      fun excludeQueryStrategies(classes: List<KClass<out QueryStrategy>>):(QueryStrategy) -> Boolean {
         return { queryStrategy: QueryStrategy -> !classes.contains(queryStrategy::class) }
      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun default(): QueryEngineFactory {
         return withOperationInvokers(
            VyneCacheConfiguration.default(),
            emptyList(),
            emptyList(),
            LocalProjectionProvider()
         )
      }

//      fun jhipster(operationInvokers: List<OperationInvoker> = DefaultInvokers.invokers): JHipsterQueryEngineFactory {
//         return JHipsterQueryEngineFactory(edgeEvaluators(operationInvokers))
//      }

      // Useful for testing.
      // For prod, use a spring-wired context,
      // which is sure to collect all strategies
      fun withOperationInvokers(
         vyneCacheConfiguration: VyneCacheConfiguration,
         formatSpecs: List<ModelFormatSpec> = emptyList(),
         vararg invokers: OperationInvoker
      ): QueryEngineFactory {
         return withOperationInvokers(
            vyneCacheConfiguration,
            invokers.toList(),
            formatSpecs,
            projectionProvider = LocalProjectionProvider()
         )
      }

      fun withOperationInvokers(
         vyneCacheConfiguration: VyneCacheConfiguration,
         invokers: List<OperationInvoker>,
         formatSpecs: List<ModelFormatSpec> = emptyList(),
         projectionProvider: ProjectionProvider = LocalProjectionProvider(),
         queryMetricsReporter: QueryMetricsReporter = NoOpMetricsReporter,
         stateStoreProvider: StateStoreProvider? = null,
         // Allows excluding query strategies from making into the query engine.
         // Only useful for testing
         queryStrategyFilter: (QueryStrategy) -> Boolean = DEFAULT_QUERY_STRATEGY_FILTER
      ): QueryEngineFactory {
         val invocationService = operationInvocationService(invokers)
         val opInvocationEvaluator = OperationInvocationEvaluator(invocationService)
         val edgeEvaluator = EdgeNavigator(edgeEvaluators(opInvocationEvaluator))
         val graphQueryStrategy = GraphSearchQueryStrategy(edgeEvaluator, vyneCacheConfiguration)

         val queryStrategies = listOf(
            ExpressionEvaluatingQueryStrategy(),
            ModelsScanStrategy(),
            StreamMergingQueryStrategy(stateStoreProvider),
            DirectServiceInvocationStrategy(invocationService),
            QueryOperationInvocationStrategy(invocationService),
            graphQueryStrategy,
            ObjectBuilderStrategy(),
         ).filter(queryStrategyFilter)

         return DefaultQueryEngineFactory(
            strategies = queryStrategies,
            projectionProvider,
            operationInvocationService = invocationService,
            formatSpecs = formatSpecs,
            metricsReporter = queryMetricsReporter
         )
      }

      private fun edgeEvaluators(operationInvocationEdgeEvaluator: EdgeEvaluator): List<EdgeEvaluator> {
         return listOf(
            StartFactEdgeEvaluator,
            NoArgServiceEdgeEvaluator,
            RequiresParameterEdgeEvaluator(),
            AttributeOfEdgeEvaluator,
            IsTypeOfEdgeEvaluator,
            HasParamOfTypeEdgeEvaluator,
            IsInstanceOfEdgeEvaluator,
            InstanceHasAttributeEdgeEvaluator,
            OperationParameterEdgeEvaluator,
            HasAttributeEdgeEvaluator(),
            CanPopulateEdgeEvaluator,
            ExtendsTypeEdgeEvaluator,
            EnumSynonymEdgeEvaluator,
            QueryBuildingEvaluator(),
            ArrayMappingAttributeEvaluator(),
            ExpressionEvaluator(),
            operationInvocationEdgeEvaluator
         )
      }

      private fun operationInvocationService(invokers: List<OperationInvoker>): OperationInvocationService {
         return DatasourceAwareOperationInvocationServiceDecorator(
            PolicyAwareOperationInvocationServiceDecorator(
               DefaultOperationInvocationService(invokers)
            )
         )
      }
   }
}

class DefaultQueryEngineFactory(
   private val strategies: List<QueryStrategy>,
   private val projectionProvider: ProjectionProvider,
   private val operationInvocationService: OperationInvocationService,
   private val formatSpecs: List<ModelFormatSpec> = emptyList(),
   private val metricsReporter: QueryMetricsReporter = NoOpMetricsReporter
) : QueryEngineFactory {

   override fun queryEngine(schema: Schema, metricsTags: Map<String, String>): QueryEngine {
      return queryEngine(schema, FactSetMap.create())
   }

   override fun queryEngine(schema: Schema, models: FactSetMap, metricsTags: Map<String, String>): StatefulQueryEngine {
      return StatefulQueryEngine(
         models,
         schema,
         strategies,
         projectionProvider = projectionProvider,
         operationInvocationService = operationInvocationService,
         formatSpecs = formatSpecs,
         metricsReporter = metricsReporter
      )
   }
}
