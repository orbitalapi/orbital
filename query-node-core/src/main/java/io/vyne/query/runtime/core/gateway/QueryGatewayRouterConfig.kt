package io.vyne.query.runtime.core.gateway

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse

/**
 * Configures a router that will accept http requests
 * for saved queries containing an HttpOperation annotation
 */
@Configuration
//@ConditionalOnProperty("vyne.query-router-url")
class QueryGatewayRouterConfig {

   @Bean
   fun buildRoutes(requestHandler: QueryRouteService): RouterFunction<ServerResponse> {
      return requestHandler.router()
   }
}
