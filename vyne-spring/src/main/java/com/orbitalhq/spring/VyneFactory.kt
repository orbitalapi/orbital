package com.orbitalhq.spring

import com.orbitalhq.Vyne
import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.VyneProvider
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.Fact
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.graph.operationInvocation.cache.OperationCacheFactory
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
      return vyne
   }

   override fun createVyne(facts: Set<Fact>, schema: Schema, queryOptions: QueryOptions): Vyne {
      return vyne
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
   private val metricsReporter: QueryMetricsReporter = NoOpMetricsReporter
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
               operationInvokers,
               operationCacheFactory.maxResultRecordCount,
               operationCacheFactory.getOperationCache(queryOptions.cachingStrategy)
            ),
            projectionProvider = projectionProvider,
            formatSpecs = formatSpecRegistry.formats,
            stateStoreProvider = stateStoreProvider,
            queryMetricsReporter = metricsReporter
         ),
      )
      facts.forEach { fact ->
         val typedInstance = TypedInstance.fromNamedType(
            TypeNamedInstance(fact.typeName, fact.value),
            vyne.schema,
            true,
            DefinedInSchema
         )
         vyne.addModel(typedInstance, fact.factSetId)
      }

      return vyne
   }
}
