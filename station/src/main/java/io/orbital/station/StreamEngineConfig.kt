package io.orbital.station

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
import com.orbitalhq.embedded.EmbeddedVyneClientWithSchema
import com.orbitalhq.pipelines.jet.StreamEngineConfigMarker
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.api.transport.PipelineJacksonModule
import com.orbitalhq.pipelines.jet.pipelines.PipelineConfigRepository
import com.orbitalhq.pipelines.jet.sink.PipelineSinkBuilder
import com.orbitalhq.pipelines.jet.sink.PipelineSinkProvider
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceProvider
import com.orbitalhq.pipelines.jet.streams.StreamStateManagerHazelcastConfig
import com.orbitalhq.pipelines.jet.streams.StreamStatusMapStore
import com.orbitalhq.pipelines.jet.streams.StreamStatusRepository
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.spring.config.EnvVariablesConfig
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.nio.file.Files

/**
 * This class contains the config required to get the stream engine running within a server
 * (either the old stream server, or Orbital station)
 */
@Configuration
@ComponentScan(basePackageClasses = [StreamEngineConfigMarker::class])
@Import(EmbeddedVyneClientWithSchema::class)
@EnableJpaRepositories(basePackageClasses = [StreamStatusRepository::class])
@EntityScan(basePackageClasses = [StreamStatus::class])
class StreamEngineConfig {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @Bean
   fun pipelineRepository(
      config: PipelineConfig,
      mapper: ObjectMapper,
      schemaChangedEventProvider: SchemaChangedEventProvider,
      envVariablesConfig: EnvVariablesConfig
   ): PipelineConfigRepository {

      val loaders = mutableListOf(
         FileConfigSourceLoader(envVariablesConfig.envVariablesPath, failIfNotFound = false, packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER),
         SchemaConfigSourceLoader(schemaChangedEventProvider, "env.conf")
      )
      loaders.add(SchemaConfigSourceLoader(schemaChangedEventProvider, "*.conf", sourceType = "@orbital/pipelines"))
      return PipelineConfigRepository(loaders)
   }

   @Bean
   fun sourceProvider(builders: List<PipelineSourceBuilder<*>>): PipelineSourceProvider {
      return PipelineSourceProvider(builders)
   }

   @Bean
   fun sinkProvider(builders: List<PipelineSinkBuilder<*, *>>): PipelineSinkProvider {
      return PipelineSinkProvider(builders)
   }

   @Bean
   fun springManagedContext(): SpringManagedContext {
      return SpringManagedContext()
   }

   @Bean
   fun instance(
      mapStore: StreamStatusMapStore,
      @Value("\${vyne.hazelcast.port:25701}") hazelcastPort: Int = 25701,
      @Value("\${vyne.hazelcast.cluster-name:orbital}") clusterName: String = "orbital",
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
