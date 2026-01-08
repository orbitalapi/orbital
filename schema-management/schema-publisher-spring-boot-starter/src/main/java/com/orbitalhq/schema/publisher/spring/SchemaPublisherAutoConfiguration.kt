package com.orbitalhq.schema.publisher.spring

import com.orbitalhq.schema.publisher.SchemaPublisherService
import com.orbitalhq.schema.publisher.SchemaPublisherTransport
import com.orbitalhq.schema.publisher.http.HttpSchemaPublisher
import com.orbitalhq.schema.publisher.http.HttpSchemaSubmitter
import com.orbitalhq.utils.orElse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * Auto-configuration for Schema Publisher.
 *
 * This configuration is activated when:
 * - orbital.schema.publisher.enabled is true (default)
 * - The necessary classes are on the classpath
 *
 * It creates:
 * - RestClient for HTTP communication
 * - HttpSchemaSubmitter for submitting schemas
 * - HttpSchemaPublisher as the SchemaPublisherTransport
 * - SchemaPublisherService
 * - SchemaLoader for automatic schema discovery
 */
@AutoConfiguration
@EnableConfigurationProperties(SchemaPublisherProperties::class)
@ConditionalOnProperty(
   prefix = "orbital.schema.publisher",
   name = ["enabled"],
   havingValue = "true",
   matchIfMissing = true
)
class SchemaPublisherAutoConfiguration {

   private val logger = KotlinLogging.logger {}

   /**
    * Creates a RestClient configured for the schema server.
    */
   @Bean
   @ConditionalOnMissingBean(name = ["schemaPublisherRestClient"])
   fun schemaPublisherRestClient(properties: SchemaPublisherProperties): RestClient {
      logger.debug { "Creating RestClient for schema server at: ${properties.url}" }

      return RestClient.builder()
         .baseUrl(properties.url)
         .build()
   }

   /**
    * Creates the HTTP schema submitter using RestClient.
    */
   @Bean
   @ConditionalOnMissingBean
   fun httpSchemaSubmitter(
      restClient: RestClient
   ): HttpSchemaSubmitter {
      logger.debug { "Creating RestClient-based HttpSchemaSubmitter" }
      return RestClientHttpSchemaSubmitter(restClient)
   }

   /**
    * Creates the HTTP-based schema publisher transport.
    */
   @Bean
   @ConditionalOnMissingBean
   fun schemaPublisherTransport(
      httpSchemaSubmitter: HttpSchemaSubmitter,
      @Value("\${orbital.schema.publisher.retry-interval:3s}") retryInterval: Duration
   ): SchemaPublisherTransport {
      logger.debug { "Creating HttpSchemaPublisher with retry interval: $retryInterval" }
      return HttpSchemaPublisher(httpSchemaSubmitter, retryInterval)
   }

   /**
    * Creates the main SchemaPublisherService bean.
    *
    * The publisher ID defaults to the spring.application.name if not explicitly configured.
    */
   @Bean
   @ConditionalOnMissingBean
   fun schemaPublisherService(
      @Value($$"${spring.application.name}") applicationName: String?,
      properties: SchemaPublisherProperties,
      transport: SchemaPublisherTransport,
      applicationContext: ApplicationContext
   ): SchemaPublisherService {
      val publisherId = publisherId(applicationName, properties, applicationContext)
      logger.info { "Creating SchemaPublisherService with publisher ID: $publisherId" }

      return SchemaPublisherService(publisherId, transport)
   }

   private fun publisherId(
      applicationName: String?,
      properties: SchemaPublisherProperties,
      applicationContext: ApplicationContext
   ): String {
      return properties.publisherId
         ?: applicationName.orEmpty().ifEmpty {
            logger.warn { "Could not find either a publisherId or an application name. You should consider setting --orbital.schema.publisher.publisherId" }
            applicationContext.applicationName.ifEmpty { applicationContext.id.orElse("unknown") }
         }

   }

   /**
    * Creates a schema loader that will discover and load Taxi schemas.
    *
    */
   @Bean
   @ConditionalOnMissingBean
   fun schemaLoader(
      publisherService: SchemaPublisherService,
      properties: SchemaPublisherProperties,
      applicationContext: ApplicationContext
   ): SchemaPublisherTask {
      val basePackages = AutoConfigurationPackages.get(applicationContext)
      return SchemaPublisherTask(
         publisherService, properties, basePackages,
      )
   }
}
