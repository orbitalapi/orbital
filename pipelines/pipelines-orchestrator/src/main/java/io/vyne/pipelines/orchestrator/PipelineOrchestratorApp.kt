package io.vyne.pipelines.orchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackageClasses = [PipelineOrchestratorController::class])
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
      fun restTemplate(objectMapper: ObjectMapper): RestTemplate {
         val template = RestTemplate()
         template.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            execution.execute(request, body);
         })

         val messageConverter = MappingJackson2HttpMessageConverter()
         messageConverter.objectMapper = objectMapper
         template.messageConverters.removeIf { it is MappingJackson2HttpMessageConverter }
         template.messageConverters.add(messageConverter)

         return template
      }

   }
}
