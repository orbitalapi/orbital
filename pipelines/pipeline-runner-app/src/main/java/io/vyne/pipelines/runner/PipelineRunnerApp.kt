package io.vyne.pipelines.runner

import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import reactivefeign.spring.config.EnableReactiveFeignClients


@SpringBootApplication
@EnableDiscoveryClient
@VyneSchemaConsumer
@EnableVyne
@EnableReactiveFeignClients(basePackageClasses = [PipelineEventsApi::class])
@Import(HttpAuthConfig::class)
@EnableConfigurationProperties(VyneSpringCacheConfiguration::class, VyneSpringProjectionConfiguration::class, VyneSpringHazelcastConfiguration::class)
class PipelineRunnerApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineRunnerApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineModule() = PipelineJacksonModule()

      /**
       * Creates an OperationInvocationService that can be used outside of Vyne.
       * We use this for calling services declared in Taxi, from within pipeline transports.
       */
      @Bean
      fun operationInvocationService(operationInvokers: List<OperationInvoker>): OperationInvocationService {
         return DefaultOperationInvocationService(operationInvokers)
      }


   }
}










