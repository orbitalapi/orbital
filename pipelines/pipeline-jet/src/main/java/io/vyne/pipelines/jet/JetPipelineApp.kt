package io.vyne.pipelines.jet

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.config.Config
import com.hazelcast.jet.Jet
import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.config.JetConfig
import com.hazelcast.spring.context.SpringManagedContext
import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.pipelines.jet.sink.PipelineSinkProvider
import io.vyne.pipelines.jet.source.PipelineSourceProvider
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.pipelines.jet.pipelines.PipelineRepository
import io.vyne.pipelines.jet.sink.PipelineSinkBuilder
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.config.DiscoveryClientConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.nio.file.Files
import java.time.Clock


@SpringBootApplication
@VyneSchemaConsumer
@EnableVyne
@EnableDiscoveryClient
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   PipelineConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneConnectionsConfig::class
)
@Import(HttpAuthConfig::class,
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
   fun pipelineRepository(
      config: PipelineConfig,
      mapper: ObjectMapper
   ): PipelineRepository {
      if (!Files.exists(config.pipelinePath)) {
         logger.info { "Pipelines config path ${config.pipelinePath.toFile().canonicalPath} does not exist, creating" }
         config.pipelinePath.toFile().mkdirs()
      } else {
         logger.info { "Using pipelines stored at ${config.pipelinePath.toFile().canonicalPath}" }
      }
      return PipelineRepository(config.pipelinePath, mapper)
   }

   @Bean
   fun clock(): Clock = Clock.systemUTC()
}


@Configuration
class JetConfiguration {
   @Bean
   fun springManagedContext(): SpringManagedContext {
      return SpringManagedContext()
   }

   @Bean
   fun instance(): JetInstance {
      // You can configure Hazelcast Jet instance programmatically
      val jetConfig = JetConfig() // configure SpringManagedContext for @SpringAware
         .configureHazelcast { hzConfig: Config ->
            hzConfig.managedContext = springManagedContext()
         }
      return Jet.newJetInstance(jetConfig)
   }

}
