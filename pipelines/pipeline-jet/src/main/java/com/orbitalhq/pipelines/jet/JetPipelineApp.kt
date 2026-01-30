package com.orbitalhq.pipelines.jet

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MapStoreConfig
import com.hazelcast.config.YamlConfigBuilder
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.context.SpringManagedContext
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.monitoring.EnableCloudMetrics
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule
import com.orbitalhq.pipelines.jet.pipelines.PipelineConfigRepository
import com.orbitalhq.pipelines.jet.sink.PipelineSinkBuilder
import com.orbitalhq.pipelines.jet.sink.PipelineSinkProvider
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceProvider
import com.orbitalhq.pipelines.jet.streams.StreamStateManagerHazelcastConfig
import com.orbitalhq.pipelines.jet.streams.StreamStatusMapStore
import com.orbitalhq.query.connectors.NoOperationInvocationEventConsumer
import com.orbitalhq.query.connectors.OperationInvocationCountingEventConsumer
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.spring.EnableVyne
import com.orbitalhq.spring.VyneSchemaConsumer
import com.orbitalhq.spring.config.DiscoveryClientConfig
import com.orbitalhq.spring.config.EnvVariablesConfig
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.http.auth.HttpAuthConfig
import com.orbitalhq.spring.http.websocket.WebSocketPingConfig
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.time.Clock


@SpringBootApplication(exclude = [MongoReactiveAutoConfiguration::class])
@VyneSchemaConsumer
@EnableVyne
@EnableDiscoveryClient
@EnableScheduling
@EnableJpaRepositories
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   VyneSpringProjectionConfiguration::class,
   VyneConnectionsConfig::class,
   EnvVariablesConfig::class
)
@Import(
   HttpAuthConfig::class,
   PipelineStateConfig::class,
   DiscoveryClientConfig::class,
   WebSocketPingConfig::class
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
   fun noOpEventConsumer():OperationInvocationCountingEventConsumer = NoOperationInvocationEventConsumer


   @Bean
   fun pipelineRepository(
      config: PipelineConfig,
      mapper: ObjectMapper,
      schemaChangedEventProvider: SchemaChangedEventProvider,
      envVariablesConfig: EnvVariablesConfig,
      @Value("\${vyne.environment.name:#{null}}") environmentName: String?
   ): PipelineConfigRepository {

      val loaders = mutableListOf<ConfigSourceLoader>(
         FileConfigSourceLoader(
            envVariablesConfig.envVariablesPath,
            failIfNotFound = false,
            packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER
         ),
         SchemaConfigSourceLoader(schemaChangedEventProvider, "env.conf", environmentName = environmentName)
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
      loaders.add(SchemaConfigSourceLoader(schemaChangedEventProvider, "*.conf", sourceType = "@orbital/pipelines", environmentName = environmentName))
      return PipelineConfigRepository(loaders)
   }

   @Bean
   fun clock(): Clock = Clock.systemUTC()
}

private val logger = KotlinLogging.logger { }

@Configuration
@EnableCloudMetrics
class JetConfiguration {
   @Bean
   fun springManagedContext(): SpringManagedContext {
      return SpringManagedContext()
   }

   @Bean
   fun instance(
      mapStore: StreamStatusMapStore,
      @Value("\${vyne.hazelcast.port:25701}") hazelcastPort: Int = 25701,
      @Value("\${vyne.hazelcast.cluster-name:dev}") clusterName: String = "orbital-stream-server",
      @Value("\${vyne.hazelcast.configYamlPath:#{null}}") configYamlPath: String? = null
   ): HazelcastInstance {
      if (configYamlPath == null) {
         logger.info { "hazelcast config yaml path is not provided, setting up multicast config with port: $hazelcastPort and cluster name $clusterName" }
         val config = Config()
         config.clusterName = clusterName
         config.networkConfig.port = hazelcastPort
         config.networkConfig.isPortAutoIncrement = true
         config.jetConfig.isEnabled = true
         config.managedContext = springManagedContext()
         config.networkConfig.join.autoDetectionConfig.setEnabled(true)
         logger.info { "setting the mapstore config for ${StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME}" }
         config.addMapConfig(getMapStoreConfig(mapStore))
         return Hazelcast.newHazelcastInstance(config)
      } else {
         logger.info { "Creating Hazelcast config from yaml file at $configYamlPath" }
         val yamlCfg = YamlConfigBuilder(configYamlPath).build()
         yamlCfg.managedContext = springManagedContext()
         logger.info { "setting the mapstore config for ${StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME}" }
         yamlCfg.addMapConfig(getMapStoreConfig(mapStore))
         return Hazelcast.newHazelcastInstance(yamlCfg)
      }
   }

   private fun getMapStoreConfig(mapStore: StreamStatusMapStore): MapConfig {
      val mapStoreConfig = MapStoreConfig().apply {
         isEnabled = true
         implementation = mapStore
         writeDelaySeconds = 0
      }

      val streamStatusMapConfig = MapConfig(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
      streamStatusMapConfig.setMapStoreConfig(mapStoreConfig)
      return streamStatusMapConfig

   }
}
