package io.vyne.spring

import io.vyne.Vyne
import io.vyne.VyneCacheConfiguration
import io.vyne.VyneProvider
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedInstance
import io.vyne.query.Fact
import io.vyne.query.QueryEngineFactory
import io.vyne.query.connectors.OperationInvoker
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.graph.operationInvocation.OperationCacheFactory
import io.vyne.query.projection.LocalProjectionProvider
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.QueryOptions
import io.vyne.schemas.Schema
import io.vyne.spring.config.ProjectionDistribution
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.projection.HazelcastProjectionProvider
import io.vyne.spring.query.formats.FormatSpecRegistry
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
   private val operationCacheFactory: OperationCacheFactory = OperationCacheFactory(),
   private val formatSpecRegistry: FormatSpecRegistry
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
               operationCache = operationCacheFactory.getCache(queryOptions.cachingStrategy)
            ),
            projectionProvider = projectionProvider,
            formatSpecs = formatSpecRegistry.formats
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
