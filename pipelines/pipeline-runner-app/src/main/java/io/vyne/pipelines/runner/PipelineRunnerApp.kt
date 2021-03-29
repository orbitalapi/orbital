package io.vyne.pipelines.runner

import io.vyne.VyneCacheConfiguration
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean


@SpringBootApplication
@EnableDiscoveryClient
@VyneSchemaConsumer
// TODO investigate why the Runner requires Vyne.
@EnableVyne
@EnableFeignClients(basePackageClasses = [PipelineEventsApi::class])
@EnableConfigurationProperties(VyneCacheConfiguration::class)
class PipelineRunnerApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineRunnerApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineModule() = PipelineJacksonModule()

   }
}










