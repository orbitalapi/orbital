package com.orbitalhq.query.runtime.core.gateway

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse

/**
 * Configures a router that will accept http requests
 * for saved queries containing an HttpOperation annotation
 */
@Configuration
@ConditionalOnProperty("vyne.dispatcher.http.enabled", havingValue = "true", matchIfMissing = false)
class QueryGatewayRouterConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   @Bean
   fun buildRoutes(requestHandler: QueryRouteService): RouterFunction<ServerResponse> {
      logger.info { "Exposing router for handling inbound queries" }
      return requestHandler.router()
   }
}
