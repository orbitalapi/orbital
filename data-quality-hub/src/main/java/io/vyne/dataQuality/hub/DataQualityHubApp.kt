package io.vyne.dataQuality.hub

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
@EnableEurekaClient
class DataQualityHubApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(DataQualityHubApp::class.java)
         app.run(*args)
      }
   }
}


@Configurable
@EnableJpaRepositories
class RepositoryConfig

@Configuration
class WebConfig : WebMvcConfigurer {
   override fun addCorsMappings(registry: CorsRegistry) {
      registry.addMapping("/**")
         .allowedOrigins("*").allowedMethods("*").allowedHeaders("*")
   }
}
