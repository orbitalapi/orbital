package org.taxilang.playground

import io.vyne.query.TaxiJacksonModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@SpringBootApplication
class TaxiPlaygroundApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(TaxiPlaygroundApp::class.java, *args)
      }
   }

   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule()

}


@Configuration
@Profile("dev")
class WebConfiguration : WebFluxConfigurer {
   override fun addCorsMappings(registry: CorsRegistry) {
      registry.addMapping("/**").allowedMethods("*")
   }
}
