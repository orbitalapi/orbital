package com.orbitalhq.pipelines.jet

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.context.SpringManagedContext
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.soap.SoapWsdlSourceConverter
import com.orbitalhq.monitoring.EnableCloudMetrics
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule
import com.orbitalhq.pipelines.jet.pipelines.PipelineConfigRepository
import com.orbitalhq.pipelines.jet.sink.PipelineSinkBuilder
import com.orbitalhq.pipelines.jet.sink.PipelineSinkProvider
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceProvider
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.spring.EnableVyne
import com.orbitalhq.spring.VyneSchemaConsumer
import com.orbitalhq.spring.config.*
import com.orbitalhq.spring.http.auth.HttpAuthConfig
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.time.Clock


@SpringBootApplication
@VyneSchemaConsumer
@EnableVyne
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   PipelineConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneConnectionsConfig::class,
   EnvVariablesConfig::class
)
@Import(
   HttpAuthConfig::class,
   PipelineStateConfig::class,
   DiscoveryClientConfig::class
)
class JetPipelineApp {
   companion object {
      private val logger = KotlinLogging.logger {}

      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(JetPipelineApp::class.java)
         app.run(*args)
      }
   }

   @Bean
   fun sourceConverterRegistry(): SourceConverterRegistry = SourceConverterRegistry(
      setOf(
         TaxiSourceConverter,
         SoapWsdlSourceConverter
      ),
      registerWithStaticRegistry = true
   )

   @Bean
   fun formatSpecRegistry(): FormatSpecRegistry = FormatSpecRegistry.default()

   @Bean
   fun pipelineModule() = PipelineJacksonModule()

   @Bean
   fun sourceProvider(builders: List<PipelineSourceBuilder<*>>): PipelineSourceProvider {
      return PipelineSourceProvider(builders)
   }

   @Bean
   fun sinkProvider(builders: List<PipelineSinkBuilder<*, *>>): PipelineSinkProvider {
      return PipelineSinkProvider(builders)
   }

   @Bean
   fun webClientCustomizer(
      loadBalancingFilterFunction: ReactorLoadBalancerExchangeFilterFunction,
      discoveryClient: DiscoveryClient

   ): WebClientCustomizer {
      return WebClientCustomizer { webClientBuilder ->
         webClientBuilder.filter(
            ConditionallyLoadBalancedExchangeFilterFunction.onlyKnownHosts(
               discoveryClient.services,
               loadBalancingFilterFunction
            )
         )
      }
   }

   @Bean
   fun pipelineRepository(
      config: PipelineConfig,
      mapper: ObjectMapper,
      schemaChangedEventProvider: SchemaChangedEventProvider,
      envVariablesConfig: EnvVariablesConfig
   ): PipelineConfigRepository {

      val loaders = mutableListOf<ConfigSourceLoader>(
         FileConfigSourceLoader(envVariablesConfig.envVariablesPath, failIfNotFound = false, packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER),
         SchemaConfigSourceLoader(schemaChangedEventProvider, "env.conf")
      )
      if (config.pipelinePath != null) {
         if (!Files.exists(config.pipelinePath)) {
            logger.info { "Pipelines config path ${config.pipelinePath.toFile().canonicalPath} does not exist, creating" }
            config.pipelinePath.toFile().mkdirs()
         } else {
            logger.info { "Using pipelines stored at ${config.pipelinePath.toFile().canonicalPath}" }
         }

         loaders.add(FileConfigSourceLoader(config.pipelinePath, packageIdentifier = PipelineConfig.PACKAGE_IDENTIFIER))
      }
      loaders.add(SchemaConfigSourceLoader(schemaChangedEventProvider, "*.conf", sourceType = "@orbital/pipelines"))
      return PipelineConfigRepository(loaders)
   }

   @Bean
   fun clock(): Clock = Clock.systemUTC()
}


@Configuration
@EnableCloudMetrics
class JetConfiguration {
   @Bean
   fun springManagedContext(): SpringManagedContext {
      return SpringManagedContext()
   }

   @Bean
   fun instance(): HazelcastInstance {
      val config = Config()
      config.jetConfig.isEnabled = true
      config.managedContext = springManagedContext()
      return Hazelcast.newHazelcastInstance(config)
   }

}
