package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.runner.PipelineRunnerApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackageClasses = [PipelineRunnerApi::class])
class PipelineOrchestratorApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineOrchestratorApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineJacksonModule() = PipelineJacksonModule()
   }
}
