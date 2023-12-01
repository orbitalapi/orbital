package com.orbitalhq.query.runtime.executor

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import com.orbitalhq.SourcePackageHasher
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.aws.dynamodb.DynamoDbInvoker
import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.graph.operationInvocation.cache.OperationCacheFactory
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalOperationCacheProvider
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.api.SchemaWithSourcesSchemaProvider
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import com.orbitalhq.spring.config.StaticServicesConfigDiscoveryClient
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.http.DefaultRequestFactory
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.spring.invokers.RestTemplateInvoker
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import mu.KotlinLogging
import org.springframework.cloud.client.discovery.DiscoveryClient
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
   private val formatSpecRegistry: FormatSpecRegistry,
   private val sourceConverterRegistry: SourceConverterRegistry
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
   fun buildVyne(message: QueryMessage): Pair<Vyne, DiscoveryClient> {
      val sources = message.sourcePackages()
      val sourcesHash = SourcePackageHasher.hash(sources)
      val schemaProvider = schemaCache.get(sourcesHash) {
         val timedSchema = measureTimedValue {
            TaxiSchema.from(sources, sourceConverters = sourceConverterRegistry.converters )
         }
         logger.info { "Building schema took ${timedSchema.duration}" }
         val schema = timedSchema.value
         SchemaWithSourcesSchemaProvider(schema, sources)
      }

      val (query, options) = schemaProvider.schema.parseQuery(message.query)
      val discoveryClient = StaticServicesConfigDiscoveryClient(message.services)
      val jdbcInvoker = buildJdbcInvoker(message.connections, schemaProvider)
      val httpInvoker = buildHttpInvoker(schemaProvider, message, discoveryClient)
      // SOAP invoker is not compatible with native builds
//      val soapInvoker = buildSoapInvoker(schemaProvider, discoveryClient)
      val dynamoInvoker = buildDynamoInvoker(message.connections, schemaProvider)
      val invokers = listOf(jdbcInvoker, httpInvoker, /* soapInvoker, */ dynamoInvoker)
      return Vyne(
         listOf(schemaProvider.schema),
         QueryEngineFactory.withOperationInvokers(
            cacheConfiguration,
            CacheAwareOperationInvocationDecorator.decorateAll(
               invokers,
               cacheProvider = LocalOperationCacheProvider.default()
            )
         ),
         formatSpecRegistry.formats
      ) to discoveryClient

   }

   private fun buildDynamoInvoker(connections: ConnectionsConfig, schemaProvider: SchemaProvider): DynamoDbInvoker {
      return DynamoDbInvoker(
         connectionRegistry =  AwsInMemoryConnectionRegistry(connections.aws.values.toList()),
         schemaProvider = schemaProvider
      )
   }

//   private fun buildSoapInvoker(
//      schemaProvider: SchemaProvider,
//      discoveryClient: StaticServicesConfigDiscoveryClient
//   ): SoapInvoker {
//      return SoapInvoker(
//         schemaProvider
//      )
//   }

   private fun buildHttpInvoker(
      schemaProvider: SchemaProvider,
      message: QueryMessage,
      discoveryClient: DiscoveryClient
   ): RestTemplateInvoker {

      val builder = webClientBuilder
         // Adding filter functions mutates the builder.
         // Be sure to clone a clean one.
         .clone()
         .filter(LoadBalancerFilterFunction(discoveryClient))

      return RestTemplateInvoker(
         schemaProvider,
         builder,
         AuthWebClientCustomizer.forTokens(message.authTokens),
         DefaultRequestFactory()

         )
   }

   private val jdbcConnectionFactoryCache = CacheBuilder.newBuilder()
      .build<Int, HikariJdbcConnectionFactory>()


   private fun buildJdbcInvoker(connections: ConnectionsConfig, schemaProvider: SchemaProvider): JdbcInvoker {

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
