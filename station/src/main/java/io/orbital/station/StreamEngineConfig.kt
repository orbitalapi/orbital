package io.orbital.station

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.context.SpringManagedContext
import com.orbitalhq.cockpit.core.connectors.hazelcast.EmbeddedHazelcastInstanceProvider
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.embedded.EmbeddedVyneClientWithSchema
import com.orbitalhq.pipelines.jet.StreamEngineConfigMarker
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.pipelines.PipelineConfigRepository
import com.orbitalhq.pipelines.jet.sink.PipelineSinkBuilder
import com.orbitalhq.pipelines.jet.sink.PipelineSinkProvider
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceProvider
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
            FileConfigSourceLoader(
                envVariablesConfig.envVariablesPath,
                failIfNotFound = false,
                packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER
            ),
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
        springManagedContext: SpringManagedContext,
        @Value("\${vyne.hazelcast.port:25701}") hazelcastPort: Int = 25701,
        @Value("\${vyne.hazelcast.cluster-name:orbital}") clusterName: String = "orbital",
        @Value("\${vyne.hazelcast.configYamlPath:#{null}}") configYamlPath: String? = null,
        @Value("\${vyne.hazelcast.configXmlPath:#{null}}") configXmlPath: String? = null
    ): HazelcastInstance {
        return EmbeddedHazelcastInstanceProvider().instance(mapStore, springManagedContext, hazelcastPort, clusterName, configYamlPath, configXmlPath)
    }
}

