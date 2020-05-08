package io.vyne.pipelines.runner

import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean


@SpringBootApplication
@EnableDiscoveryClient
// TODO : This annotation is misleading, I think there's a better one for clients to use, but
// @EnableVyneClient didn't work. Need to investigate
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
@EnableFeignClients(basePackageClasses = [PipelineEventsApi::class])
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










