package io.vyne.query.runtime.http

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.cache.CacheBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.SourcePackageHasher
import io.vyne.Vyne
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnections
import io.vyne.query.QueryEngineFactory
import io.vyne.query.runtime.QueryMessage
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaWithSourcesSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.config.StaticServicesConfigDiscoveryClient
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.http.DefaultRequestFactory
import io.vyne.spring.http.auth.AuthTokenInjectingRequestFactory
import io.vyne.spring.invokers.RestTemplateInvoker
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

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
   private val cacheConfiguration: VyneSpringCacheConfiguration
//   private val schemaCache: ?
) {

   private val schemaCache = CacheBuilder.newBuilder()
      .maximumSize(5)
      .build<String, SchemaProvider>()

   private val lenientObjectMapper = objectMapper
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

   fun buildVyne(message: QueryMessage): Vyne {
      val sourcesHash = SourcePackageHasher.hash(message.sourcePackages)
      val schemaProvider = schemaCache.get(sourcesHash) {
         val schema = TaxiSchema.from(message.sourcePackages)
         SchemaWithSourcesSchemaProvider(schema, message.sourcePackages)
      }

      val jdbcInvoker = buildJdbcInvoker(message.connections, schemaProvider)
      val httpInvoker = buildHttpInvoker(schemaProvider, message)

      return Vyne(
         listOf(schemaProvider.schema),
         QueryEngineFactory.withOperationInvokers(
            cacheConfiguration,
            listOf(
               jdbcInvoker, httpInvoker
            )
         )
      )

   }

   private fun buildHttpInvoker(
      schemaProvider: SchemaProvider,
      message: QueryMessage
   ): RestTemplateInvoker {
      val discoveryClient = StaticServicesConfigDiscoveryClient(message.services)
      val builder = webClientBuilder.filter(LoadBalancerFilterFunction(discoveryClient))

      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         message.authTokens
      )
      return RestTemplateInvoker(
         schemaProvider,
         builder,
         emptyList(),
         requestFactory
      )
   }

   private fun buildJdbcInvoker(connections: Map<String, Any>, schemaProvider: SchemaProvider): JdbcInvoker {

      val jdbcConnections = lenientObjectMapper.convertValue<JdbcConnections>(connections)
      val connectionRegistry = InMemoryJdbcConnectionRegistry(jdbcConnections.jdbc.values.toList())
      val jdbcConnectionFactory = HikariJdbcConnectionFactory(
         connectionRegistry,
         hikariConfig,
         MicrometerMetricsTrackerFactory(meterRegistry)
      )
      return JdbcInvoker(jdbcConnectionFactory, schemaProvider)
   }

}
