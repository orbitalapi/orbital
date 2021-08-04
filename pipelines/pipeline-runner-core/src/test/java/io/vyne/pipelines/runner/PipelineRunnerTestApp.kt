package io.vyne.pipelines.runner

import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.models.json.parseKeyValuePair
import io.vyne.pipelines.AlwaysUpPipelineTransportMonitor
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import io.vyne.pipelines.runner.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.runner.transport.direct.DirectInputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputBuilder
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportOutputSpec
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.Schema
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
      fun directOutputBuilder(): DirectOutputBuilder {
         return DirectOutputBuilder(healthMonitor = AlwaysUpPipelineTransportMonitor())
      }

      @Bean
      fun directInputBuilder() : DirectInputBuilder = DirectInputBuilder()

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

      // Hack to make the stub service injectable into tests
      @Bean
      fun vyneAndStub(): VyneAndStub {
         val (vyne, stub) = PipelineTestUtils.pipelineTestVyne()
         return VyneAndStub(vyne, stub)
      }

      @Bean
      @Primary
      fun schemaProvider(vyne:Vyne):SchemaProvider {
         return SimpleSchemaProvider(vyne.schema)
      }

      // Only for tests - never really do this in an app
      @Bean
      fun schema(vyne: Vyne):Schema = vyne.schema

      // Only for tests - never really do this in an app
      @Bean
      fun vyne(vyneAndStub: VyneAndStub): Vyne = vyneAndStub.vyne

      // Stubbed for tests.
      @Bean
      @Primary
      fun operationInvocationService(stub:StubService):OperationInvocationService {
         return DefaultOperationInvocationService(listOf(stub))
      }

      @Bean
      @Primary
      fun vyneProvider(vyneAndStub: VyneAndStub): VyneProvider {
         val (vyne, stub) = vyneAndStub
         stub.addResponse("getUserNameFromId", vyne.parseKeyValuePair("Username", "Jimmy Pitt"))
         return SimpleVyneProvider(vyne)
      }

      @Bean
      fun stubService(vyneAndStub: VyneAndStub): StubService = vyneAndStub.stub
   }

}

data class VyneAndStub(val vyne: Vyne, val stub: StubService)
