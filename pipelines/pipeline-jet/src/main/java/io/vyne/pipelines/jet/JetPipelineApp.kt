package io.vyne.pipelines.jet

import com.hazelcast.config.Config
import com.hazelcast.jet.Jet
import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.config.JetConfig
import com.hazelcast.spring.context.SpringManagedContext
import io.vyne.pipelines.jet.sink.PipelineSinkProvider
import io.vyne.pipelines.jet.source.PipelineSourceProvider
import io.vyne.pipelines.jet.api.transport.PipelineJacksonModule
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock


@SpringBootApplication
@VyneSchemaConsumer
@EnableVyne
@EnableDiscoveryClient
@EnableConfigurationProperties(VyneSpringCacheConfiguration::class, PipelineConfig::class, VyneSpringProjectionConfiguration::class)
@Import(HttpAuthConfig::class, PipelineStateConfig::class)
class JetPipelineApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(JetPipelineApp::class.java)
         app.run(*args)
      }
   }

   @Bean
   fun pipelineModule() = PipelineJacksonModule()

   @Bean
   fun sourceProvider():PipelineSourceProvider = PipelineSourceProvider.default()

   @Bean
   fun sinkProvider():PipelineSinkProvider = PipelineSinkProvider.default()

   @Bean
   fun clock():Clock = Clock.systemUTC()
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
