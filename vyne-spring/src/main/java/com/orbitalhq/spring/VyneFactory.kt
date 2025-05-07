package com.orbitalhq.spring

import com.orbitalhq.Vyne
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneProvider
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.query.EmitMetrics
import com.orbitalhq.query.Fact
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.connectors.CountingOperationInvokerDecorator
import com.orbitalhq.query.connectors.NoOperationInvocationEventConsumer
import com.orbitalhq.query.connectors.OperationInvocationCountingEventConsumer
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.graph.operationInvocation.cache.OperationCacheFactory
import com.orbitalhq.query.planner.QueryPlanner
import com.orbitalhq.query.projection.LocalProjectionProvider
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.spring.config.ProjectionDistribution
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.projection.HazelcastProjectionProvider
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import org.springframework.beans.factory.FactoryBean

// To make testing easier
class SimpleVyneProvider(private val vyne: Vyne) : VyneProvider {
   override fun createVyne(facts: Set<Fact>): Vyne {
      if (facts.isEmpty()) {
         return vyne
      } else {
         val clone = vyne.clone()
         facts.forEach { clone.addModel(it.toTypedInstance(clone.schema), it.factSetId) }
         return clone
      }

   }

   override fun createVyne(facts: Set<Fact>, schema: Schema, queryOptions: QueryOptions): Vyne {
      if (facts.isEmpty()) {
         return vyne
      } else {
         val clone = vyne.clone(schema)
         facts.forEach { clone.addModel(it.toTypedInstance(clone.schema), it.factSetId) }
         return clone
      }
   }
}

class VyneFactory(
   private val schemaProvider: SchemaProvider,
   private val operationInvokers: List<OperationInvoker>,
   private val vyneCacheConfiguration: VyneCacheConfiguration,
   private val vyneSpringProjectionConfiguration: VyneSpringProjectionConfiguration,
   private val operationCacheFactory: OperationCacheFactory = OperationCacheFactory.default(),
   private val formatSpecRegistry: FormatSpecRegistry,
   private val stateStoreProvider: StateStoreProvider? = null,
   private val metricsReporter: QueryMetricsReporter = NoOpMetricsReporter,
   private val queryPlanner: QueryPlanner = QueryPlanner(),
   private val operationInvocationEventConsumer: OperationInvocationCountingEventConsumer = NoOperationInvocationEventConsumer
) : FactoryBean<Vyne>, VyneProvider {

   override fun isSingleton() = true
   override fun getObjectType() = Vyne::class.java

   override fun getObject(): Vyne {
      return buildVyne()
   }

   // For readability
   override fun createVyne(facts: Set<Fact>) = buildVyne(facts)
   override fun createVyne(facts: Set<Fact>, schema: Schema, queryOptions: QueryOptions): Vyne {
      return buildVyne(facts, schema, queryOptions)
   }

   override fun emitMetrics(queryResult: QueryResult, emitMetrics: EmitMetrics) {
      // MP: 1-May-25: This is part of a suboptimal solution, as we end up montioring
      // results and errors differently.
      // let's get rid of it asap
      // Need a way of monitoring the error stream from the
      // QueryResult that only monitors the "outer" query stream
      if (emitMetrics.results) {
         TODO("Emitting results not implemented here")
      }
      if (emitMetrics.errors) {
         metricsReporter.observeErrorStream(queryResult.errors)
      }
   }

   private fun buildVyne(
      facts: Set<Fact> = emptySet(),
      schema: Schema = schemaProvider.schema,
      queryOptions: QueryOptions = QueryOptions.default()
   ): Vyne {
      val projectionProvider =
         if (vyneSpringProjectionConfiguration.distributionMode == ProjectionDistribution.DISTRIBUTED)
            HazelcastProjectionProvider(
               taskSize = vyneSpringProjectionConfiguration.distributionPacketSize,
               nonLocalDistributionClusterSize = vyneSpringProjectionConfiguration.distributionRemoteBias
            )
         else LocalProjectionProvider()

      val vyne = Vyne(
         schemas = listOf(schema),
         queryEngineFactory = QueryEngineFactory.withOperationInvokers(
            vyneCacheConfiguration,
            CacheAwareOperationInvocationDecorator.decorateAll(
               CountingOperationInvokerDecorator.decorateAll(
                  operationInvokers, operationInvocationEventConsumer
               ),
               vyneCacheConfiguration.operationCache.maxCachedOperations,
               operationCacheFactory.getOperationCache(queryOptions.cachingStrategy)
            ),
            projectionProvider = projectionProvider,
            formatSpecs = formatSpecRegistry.formats,
            stateStoreProvider = stateStoreProvider,
            queryMetricsReporter = metricsReporter
         ),
         queryPlanner = queryPlanner,
         formatSpecs = formatSpecRegistry.formats
      )
      facts.forEach { fact ->
         val typedInstance = fact.toTypedInstance(schema)
         vyne.addModel(typedInstance, fact.factSetId)
      }

      return vyne
   }
}
