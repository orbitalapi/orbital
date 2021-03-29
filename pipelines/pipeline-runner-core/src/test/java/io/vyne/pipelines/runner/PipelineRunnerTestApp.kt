package io.vyne.pipelines.runner

import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.pipelines.runner.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportOutputSpec
import io.vyne.spring.SimpleVyneProvider
import io.vyne.spring.VyneProvider
import io.vyne.spring.VyneSchemaPublisher
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.server.RouterFunctions


@SpringBootApplication
@EnableDiscoveryClient
@VyneSchemaPublisher
@EnableFeignClients(basePackageClasses = [PipelineEventsApi::class])
class PipelineRunnerTestApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineRunnerTestApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineModule() = PipelineJacksonModule(
         listOf(
            KafkaTransportInputSpec.specId,
            CaskTransportOutputSpec.specId,
            KafkaTransportOutputSpec.specId
         )
      )

      @Bean
      fun resRouter() = RouterFunctions.resources("/static/**", ClassPathResource("static/"))

      @Bean
      fun restTemplate(): RestTemplate? {
         return RestTemplate()
      }

      @Bean
      @Primary
      fun vyneProvider(): VyneProvider {
         val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
         stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
         return SimpleVyneProvider(vyne)
      }
   }
}
