package io.vyne.cask

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.vyne.cask.query.CaskApiHandler
import io.vyne.cask.rest.CaskRestController
import io.vyne.cask.services.CaskServiceSchemaGenerator.Companion.CaskApiRootPath
import io.vyne.cask.websocket.CaskWebsocketHandler
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct


@SpringBootApplication
@EnableDiscoveryClient
@VyneSchemaPublisher
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

   @PostConstruct
   fun setUtcTimezone() {
      log().info("Setting default TimeZone to UTC")
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
      // Date values are stored in Postgresql in UTC.
      // However before sending dates to user a conversion happens (PgResultSet.getDate..) that includes default timezone
      // E.g. date 2020.03.29:00:00:00 in DB can converted and returned to user as 2020.03.28:23:00:00
      // This fix forces default timezone to be UTC
      // Alternatively we could provide -Duser.timezone=UTC at startup
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
   fun websocketHandlerAdapter(webSocketService: WebSocketService) = WebSocketHandlerAdapter(webSocketService)

   @Bean
   fun webSocketService(
      @Value("\${cask.maxTextMessageBufferSize}")  maxTextMessageBufferSize: Int,
      @Value("\${cask.maxBinaryMessageBufferSize}")  maxBinaryMessageBufferSize: Int): WebSocketService {
      val strategy = TomcatRequestUpgradeStrategy()
      strategy.maxTextMessageBufferSize = maxTextMessageBufferSize
      strategy.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize
      return HandshakeWebSocketService(strategy)
   }

   @Bean
   fun caskRouter(caskApiHandler: CaskApiHandler, caskRestController: CaskRestController) = router {
      CaskApiRootPath.nest {
         accept(APPLICATION_JSON).nest {
            GET("**", caskApiHandler::findBy)
            POST("**", caskApiHandler::findBy)
         }
      }
      resources("/static/**", ClassPathResource("static/"))
   }

   @Bean
   fun timedAspect(registry: MeterRegistry): TimedAspect? {
      capturePercentilesForAllTimers(registry)
      return TimedAspect(registry)
   }

   private fun capturePercentilesForAllTimers(registry: MeterRegistry) {
      log().info("Configuring Metrics Registry to capture percentiles for all timers.")
      registry.config().meterFilter(
         object : MeterFilter {
            override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig {
               // https://github.com/micrometer-metrics/micrometer-docs/blob/master/src/docs/concepts/histogram-quantiles.adoc
               // all timers will be created with percentiles
               // individual filtering can be done via (id.name.startsWith("reactor.onNext.delay"))
               return if (id.type == Meter.Type.TIMER) {
                  DistributionStatisticConfig.builder()
                     .percentiles(0.5, 0.9, 0.95, 0.99)
                     .build()
                     .merge(config)
               } else config
            }
         })
   }
}
