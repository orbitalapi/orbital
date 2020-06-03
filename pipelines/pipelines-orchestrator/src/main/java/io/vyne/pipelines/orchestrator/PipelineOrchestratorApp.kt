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
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


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

      @Override
      fun registerWebSocketHandlers( registry: WebSocketHandlerRegistry) {
         registry.addHandler( object: WebSocketHandler {
            override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
               TODO("Not yet implemented")
            }

            override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
               TODO("Not yet implemented")
            }

            override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
               TODO("Not yet implemented")
            }

            override fun afterConnectionEstablished(session: WebSocketSession) {
               TODO("Not yet implemented")
            }

            override fun supportsPartialMessages(): Boolean {
               TODO("Not yet implemented")
            }
         }, "/logs").setAllowedOrigins("*");
      }

   }
}
