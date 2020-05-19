package io.vyne.cask

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.cask.query.CaskApiHandler
import io.vyne.cask.query.CaskServiceSchemaGenerator.Companion.CaskApiRootPath
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.router
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import java.time.Duration


@SpringBootApplication
@EnableDiscoveryClient
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
@EnableWebFlux
@EnableAspectJAutoProxy
class CaskApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(CaskApp::class.java)
         app.webApplicationType = WebApplicationType.REACTIVE
         app.run(*args)
      }
   }

   @Bean
   fun handlerMapping(caskWebsocketHandler: CaskWebsocketHandler): HandlerMapping {
      val handlerMapping = SimpleUrlHandlerMapping()
      handlerMapping.urlMap = mapOf("/cask/**" to caskWebsocketHandler)
      handlerMapping.order = 1
      return handlerMapping
   }

   @Bean
   fun restTemplateBuilder(): RestTemplateBuilder {
      val restTemplateBuilder = RestTemplateBuilder()
      val interceptor = ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
         request.headers.add("user-agent", "CaskApp")
         execution.execute(request, body)
      }
      restTemplateBuilder.additionalInterceptors(interceptor)
      restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5))
      restTemplateBuilder.setReadTimeout(Duration.ofSeconds(5))
      return restTemplateBuilder
   }

   @Bean
   fun websocketHandlerAdapter() = WebSocketHandlerAdapter()

   @Bean
   fun caskRouter(caskApiHandler: CaskApiHandler) = router {
      CaskApiRootPath.nest {
         accept(APPLICATION_JSON).nest {
            GET("**", caskApiHandler::findBy)
         }
      }
      resources("/static/**", ClassPathResource("static/"))
   }

   @Bean
   fun timedAspect(registry: MeterRegistry?): TimedAspect? {
      return TimedAspect(registry)
   }
}
