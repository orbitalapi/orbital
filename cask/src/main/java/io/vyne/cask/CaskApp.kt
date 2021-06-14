package io.vyne.cask

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.vyne.cask.config.CaskQueryOptions
import io.vyne.cask.ddl.views.CaskViewConfig
import io.vyne.cask.query.CaskApiHandler
import io.vyne.cask.query.generators.OperationGeneratorConfig
import io.vyne.cask.rest.CaskRestController
import io.vyne.cask.services.CaskServiceSchemaGenerator.Companion.CaskApiRootPath
import io.vyne.cask.websocket.CaskWebsocketHandler
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTags
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.server.PathContainer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.util.StringUtils
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.router
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy
import org.springframework.web.server.ServerWebExchange
import java.sql.Timestamp
import java.time.Duration
import java.util.*
import java.util.regex.Pattern
import javax.annotation.PostConstruct


@SpringBootApplication
@EnableAspectJAutoProxy
@EnableConfigurationProperties(CaskViewConfig::class, OperationGeneratorConfig::class, CaskQueryOptions::class)
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
   @Qualifier("ingesterMapper")
   fun ingesterMapper(): ObjectMapper {
      val mapper: ObjectMapper = jacksonObjectMapper()
      mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
      return mapper
   }
}

@Configuration
class WebFluxWebConfig(@Value("\${cask.maxTextMessageBufferSize}") val maxTextMessageBufferSize: Int) : WebFluxConfigurer {
   override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
      // Injecting ObjectMapper into this function doesn't work as for some reason the ObjectMapper in the spring context
      // only contains the KotlinModule, so serialisation for JDK 8 temporal types got broken.
      // Below is the way webflux encoders / decoders instantiate their ObjectMappers, and 'build()' function simply goes through
      // the classpath and discovers all available Jackson modules in which case relevant JDK 8 jackson modules are discovered properly.
      val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
      val sqlTimeStampSerialiserModule = SimpleModule()
      sqlTimeStampSerialiserModule.addSerializer(java.sql.Timestamp::class.java, SqlTimeStampSerialiser())
      objectMapper.registerModule(sqlTimeStampSerialiserModule)
      configurer.defaultCodecs().maxInMemorySize(maxTextMessageBufferSize)

      configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, APPLICATION_JSON))
      configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper,
         APPLICATION_JSON,
         ActuatorV2MediaType,
         ActuatorV3MediaType))
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
      @Value("\${cask.maxTextMessageBufferSize}") maxTextMessageBufferSize: Int,
      @Value("\${cask.maxBinaryMessageBufferSize}") maxBinaryMessageBufferSize: Int): WebSocketService {
      val strategy = TomcatRequestUpgradeStrategy()
      strategy.maxTextMessageBufferSize = maxTextMessageBufferSize
      strategy.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize
      return HandshakeWebSocketService(strategy)
   }


   @Bean
   fun caskRouter(caskApiHandler: CaskApiHandler, caskRestController: CaskRestController) = router {
      CaskApiRootPath.nest {
         accept(APPLICATION_JSON,TEXT_EVENT_STREAM).nest {
            GET("**", caskApiHandler::findBy)
            POST("**", caskApiHandler::findBy)
         }
      }
      resources("/static/**", ClassPathResource("static/"))
   }

   companion object {
      private val ActuatorV2MediaType = MediaType("application", "vnd.spring-boot.actuator.v2+json")
      private val ActuatorV3MediaType = MediaType("application" , "vnd.spring-boot.actuator.v3+json")
   }
}

class SqlTimeStampSerialiser: StdSerializer<Timestamp>(Timestamp::class.java) {
   private val instantSerialiser = InstantSerializer.INSTANCE
   override fun serialize(value: Timestamp, generator: JsonGenerator, p2: SerializerProvider) {
      instantSerialiser.serialize(value.toInstant(), generator, p2)
   }
}
// Marker configration classes, to make app more testable

@EnableDiscoveryClient
@Configuration
class DiscoveryConfig

@Configuration
@EnableAsync
class AsyncConfig

@VyneSchemaPublisher
@VyneSchemaConsumer
@Configuration
class VyneConfig

@EnableWebFlux
@Configuration
class WebConfig

@EnableJpaRepositories
@Configuration
class RepositoryConfig
