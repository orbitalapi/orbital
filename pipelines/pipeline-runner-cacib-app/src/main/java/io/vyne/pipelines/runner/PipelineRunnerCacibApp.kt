package io.vyne.pipelines.runner

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.discovery.EurekaClient
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.pipelines.runner.transport.cask.CaskOutputBuilder
import io.vyne.pipelines.runner.transport.kafka.KafkaInput
import io.vyne.pipelines.runner.transport.kafka.KafkaInputBuilder
import io.vyne.pipelines.runner.transport.kafka.KafkaOutputBuilder
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component


@SpringBootApplication
@EnableDiscoveryClient
// TODO : This annotation is misleading, I think there's a better one for clients to use, but
// @EnableVyneClient didn't work. Need to investigate
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
@EnableFeignClients(basePackageClasses = [PipelineEventsApi::class])
class PipelineRunnerCacibApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineRunnerCacibApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineModule() = PipelineJacksonModule()

   }
}

@Component
class CacibKafkaInputBuilder(objectMapper: ObjectMapper) : KafkaInputBuilder(objectMapper) {

   override fun build(spec: KafkaTransportInputSpec): KafkaInput = KafkaInput(spec, objectMapper) { root ->

      var fields = checkNotNull(root.at("/fields"), { "Fields 'fields' is not an array" })

      check(fields.isArray, { "Invalid incoming format message. Fields 'fields' is not an array" })

      fields.first { it.get("name").asText() == "data" } .get("value")
   }
}

@Component
class CacibKafkaOutputuilder(objectMapper: ObjectMapper) : KafkaOutputBuilder(objectMapper)  // FIME override the other bean instead of "Component"



