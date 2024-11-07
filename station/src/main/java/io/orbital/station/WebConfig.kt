package io.orbital.station

import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.cockpit.core.WebUiUrlSupportFilter
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.serde.TaxiJacksonModule
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.web.reactive.filter.OrderedWebFilter
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Configuration
@Import(WebUiUrlSupportFilter::class)
class WebConfig(private val objectMapper: ObjectMapper) : WebFluxConfigurer {

   @Value("\${orbital.cors.enabled:true}")
   var corsEnabled: Boolean = true

   private val logger = KotlinLogging.logger {}
   override fun addCorsMappings(registry: CorsRegistry) {
      if (!corsEnabled) {
         logger.warn { "CORS is disabled.  Allowing all access" }
         registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedHeaders("*")
            .exposedHeaders("*")
            .allowedMethods("*")
      }
   }

   override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
      val defaults: ServerCodecConfigurer.ServerDefaultCodecs = configurer.defaultCodecs()
      defaults.jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
      // Spring Boot Admin 2.x
      // checks for the content-type application/vnd.spring-boot.actuator.v2.
      // If this content-type is absent, the application is considered to be a Spring Boot 1 application.
      // Spring Boot Admin can't display the metrics with Metrics are not supported for Spring Boot 1.x applications.
      defaults.jackson2JsonEncoder(
         Jackson2JsonEncoder(
            objectMapper,
            MediaType.APPLICATION_JSON,
            ActuatorV2MediaType,
            ActuatorV3MediaType
         )
      )
   }


   //   @LoadBalanced
   @Bean
   fun webClientCustomizer(
      loadBalancingFilterFunction: ReactorLoadBalancerExchangeFilterFunction,
      discoveryClient: DiscoveryClient

   ): WebClientCustomizer {
      return WebClientCustomizer { webClientBuilder ->
         webClientBuilder.filter(
            LoadBalancerFilterFunction(
               discoveryClient,
            )
         )
      }
   }


   companion object {
      private val ActuatorV2MediaType = MediaType("application", "vnd.spring-boot.actuator.v2+json")
      private val ActuatorV3MediaType = MediaType("application", "vnd.spring-boot.actuator.v3+json")
   }
}


@Configuration
class JacksonConfig {
   @Bean
   fun csvFormatSpec(): ModelFormatSpec = CsvFormatSpec


   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule


   @Bean
   fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
      return Jackson2ObjectMapperBuilderCustomizer { builder ->
         builder.featuresToEnable(
            MapperFeature.DEFAULT_VIEW_INCLUSION
         ).postConfigurer { objectMapper ->
            // Set the max length for Long's being read.
            // Without this, numbers with infinite decimals cause exceptions
            // when deserialized (eg., reading lineage back)
            objectMapper.factory.setStreamReadConstraints(
               StreamReadConstraints.defaults().rebuild().maxNumberLength(20000).build()
            )
         }
      }
   }
}

/**
 * Logs requests.
 * To enable, set log level for io.orbital.station.RequestLoggingFilter=TRACE
 */
@Component
class RequestLoggingFilter : WebFilter, OrderedWebFilter {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
      logger.trace {
         val headers = exchange.request.headers.map { (key, value) -> "[$key: ${value.joinToString(",")}]" }
            .joinToString(" , ")
         "[${exchange.request.id}] Request ${exchange.request.method.name()} : ${exchange.request.path} {Headers: $headers}"
      }
      return chain.filter(exchange)
         .doOnSuccess {
            logger.trace {
               "[${exchange.request.id}] Response ${exchange.response.statusCode}"
            }

         }
   }

   // Highest precendence to ensure we go before spring security filters
   // As we want to log requests that were rejected as well
   override fun getOrder(): Int = OrderedWebFilter.HIGHEST_PRECEDENCE

}
