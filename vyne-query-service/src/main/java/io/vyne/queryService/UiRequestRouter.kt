package io.vyne.queryService

import io.vyne.utils.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

/**
 * Handles Webflux route requests that should be handled by the
 */
@Configuration
@Order(100)
class UiRequestRouter {
      @Bean
   fun handleUiRouteRequests(): RouterFunction<ServerResponse> {
      return RouterFunctions.route(
         { request ->
            log().info("Processing request")
            !request.path().startsWith("/api")
         },
         { request ->
            TODO()
         }

      )

   }
//   @Bean
//   fun staticResourceRouter(): RouterFunction<ServerResponse> {
//      return RouterFunctions.resources("/**", ClassPathResource("static/index.html"))
//   }
}
