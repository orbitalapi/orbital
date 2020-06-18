package io.vyne.queryService

import io.vyne.query.VyneJacksonModule
import io.vyne.search.embedded.EnableVyneEmbeddedSearch
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VyneSchemaPublisher
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*


@SpringBootApplication
@EnableConfigurationProperties(QueryServerConfig::class)
@EnableVyneEmbeddedSearch
@VyneSchemaPublisher(publicationMethod = SchemaPublicationMethod.DISTRIBUTED)
class QueryServiceApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(QueryServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }

   @Bean
   fun vyneJacksonModule() = VyneJacksonModule()

   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule()

   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val version = buildInfo?.version ?: "Dev version";
      log().info("Vyne query server $version")
   }

   @Configuration
   //   @EnableWebMvc
   class WebConfig : WebMvcConfigurer {

      @Value("\${cors.host:localhost}")
      lateinit var allowedHost: String

      @Value("\${cors.enabled:false}")
      var corsEnabled: Boolean = false

      override fun addCorsMappings(registry: CorsRegistry) {
         if (corsEnabled) {
            log().info("Registering Cors host at $allowedHost")
            registry.addMapping("/**")
               .allowedOrigins(allowedHost)
         }
      }

      //      @Override
      //      public void addResourceHandlers(ResourceHandlerRegistry registry) {
      //         if (!registry.hasMappingForPattern("/**")) {
      //            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
      //         }
      //      }
      @Bean
       fun supportPathBasedLocationStrategyWithoutHashes(): ErrorViewResolver? {
         return ErrorViewResolver { request,
                                    status,
                                    model ->if (request.requestURI.contains("api")) ModelAndView("forward:http://localhost:9022",  emptyMap<String, Any>(), HttpStatus.OK) else ModelAndView("forward: index.html",  emptyMap<String, Any>(), HttpStatus.OK) }
      }
   }
}

object AuthHeaders {
   val AUTH_HEADER_NAME = "Authorization"
}

@ConfigurationProperties(prefix = "vyne")
class QueryServerConfig {
   var newSchemaSubmissionEnabled: Boolean = false
}


