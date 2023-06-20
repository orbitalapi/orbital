package io.vyne.query.runtime.executor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.SourcePackageHasher
import io.vyne.Vyne
import io.vyne.connectors.config.ConnectorsConfig
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.query.QueryEngineFactory
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.graph.operationInvocation.OperationCacheFactory
import io.vyne.query.runtime.QueryMessage
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaWithSourcesSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.config.StaticServicesConfigDiscoveryClient
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.http.DefaultRequestFactory
import io.vyne.spring.http.auth.AuthTokenInjectingRequestFactory
import io.vyne.spring.invokers.RestTemplateInvoker
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * Vyne factory that doesn't use any shared state.
 *
 * Callers are required to provide everything - including
 * schema, auth tokens, connection config, services config, etc.
 *
 * Used when running Vyne in a shared / standalone environment
 * (such as a Lambda, or a query node servicing multiple schemas)
 */
@Component
class StandaloneVyneFactory(
   private val hikariConfig: HikariConfig,
   private val meterRegistry: MeterRegistry,
   objectMapper: ObjectMapper,
   private val webClientBuilder: WebClient.Builder,
   private val cacheConfiguration: VyneSpringCacheConfiguration,
   private val operationCacheFactory: OperationCacheFactory = OperationCacheFactory()
//   private val schemaCache: ?
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val schemaCache = CacheBuilder.newBuilder()
      .maximumSize(5)
      .build<String, SchemaProvider>()

   private val lenientObjectMapper = objectMapper
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

   @OptIn(ExperimentalTime::class)
   fun buildVyne(message: QueryMessage): Vyne {
      val sources = message.sourcePackages()
      val sourcesHash = SourcePackageHasher.hash(sources)
      val schemaProvider = schemaCache.get(sourcesHash) {
         val timedSchema = measureTimedValue {
            TaxiSchema.from(sources)
         }
         logger.info { "Building schema took ${timedSchema.duration}" }
         val schema = timedSchema.value
         SchemaWithSourcesSchemaProvider(schema, sources)
      }

      val (query, options) = schemaProvider.schema.parseQuery(message.query)

      val jdbcInvoker = buildJdbcInvoker(message.connections, schemaProvider)
      val httpInvoker = buildHttpInvoker(schemaProvider, message)

      val invokers = listOf(jdbcInvoker, httpInvoker)
      return Vyne(
         listOf(schemaProvider.schema),
         QueryEngineFactory.withOperationInvokers(
            cacheConfiguration,
            CacheAwareOperationInvocationDecorator.decorateAll(
               invokers,
               operationCache = operationCacheFactory.getCache(options.cachingStrategy)
            )
         )
      )

   }

   private fun buildHttpInvoker(
      schemaProvider: SchemaProvider,
      message: QueryMessage
   ): RestTemplateInvoker {
      val discoveryClient = StaticServicesConfigDiscoveryClient(message.services)
      val builder = webClientBuilder
         // Adding filter functions mutates the builder.
         // Be sure to clone a clean one.
         .clone()
         .filter(LoadBalancerFilterFunction(discoveryClient))

      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         message.authTokens
      )
      return RestTemplateInvoker(
         schemaProvider,
         builder,
         requestFactory
      )
   }

   private val jdbcConnectionFactoryCache = CacheBuilder.newBuilder()
      .build<Int, HikariJdbcConnectionFactory>()


   private fun buildJdbcInvoker(connections: ConnectorsConfig, schemaProvider: SchemaProvider): JdbcInvoker {

      val jdbcConnectionFactory = jdbcConnectionFactoryCache.get(connections.jdbcConnectionsHash) {
         val connectionRegistry = InMemoryJdbcConnectionRegistry(connections.jdbc.values.toList())
         HikariJdbcConnectionFactory(
            connectionRegistry,
            hikariConfig,
            MicrometerMetricsTrackerFactory(meterRegistry)
         )
      }

      return JdbcInvoker(jdbcConnectionFactory, schemaProvider)
   }

}
