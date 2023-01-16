package io.vyne.queryProxy

import io.vyne.queryProxy.router.QueryRequestHandler
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.config.DiscoveryClientConfig
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse

@SpringBootApplication
@Import(
   DiscoveryClientConfig::class
)
class QueryProxyServiceApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(QueryProxyServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }
}

@VyneSchemaConsumer
@Configuration
class VyneConfig

@Configuration
class WebRouteConfig {

   @Bean
   fun buildRoutes(requestHandler: QueryRequestHandler): RouterFunction<ServerResponse> {
      return requestHandler.router()
   }
}
