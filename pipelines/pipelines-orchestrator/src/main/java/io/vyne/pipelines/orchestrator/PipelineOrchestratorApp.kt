package io.vyne.pipelines.orchestrator

import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackageClasses = [PipelineRunnerApi::class])
@EnableScheduling
class PipelineOrchestratorApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineOrchestratorApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineJacksonModule() = PipelineJacksonModule()

      @Bean
      fun restTemplate(): RestTemplate {
         val template = RestTemplate()
         template.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.set("content-type", "application/json");
            execution.execute(request, body);
         })
         return template
      }



   }
}
