package io.vyne.queryService

import io.vyne.spring.EnableVyne
import io.vyne.spring.RemoteSchemaStoreType
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
class QueryServiceApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(QueryServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }

   @Autowired(required = false)
   var buildInfo: BuildProperties? = null;

   @Autowired
   fun logInfo() {
      val version = if (buildInfo != null) "v${buildInfo!!.version}" else "Dev version";
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
               .exposedHeaders(AuthHeaders.AUTH_HEADER_NAME)
               .allowedHeaders("Authorization", "Cache-Control", "Content-Type")
               .allowCredentials(true)
               .allowedMethods("*")
         }
      }

      //      @Override
      //      public void addResourceHandlers(ResourceHandlerRegistry registry) {
      //         if (!registry.hasMappingForPattern("/**")) {
      //            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
      //         }
      //      }


   }
}

object AuthHeaders {
   val AUTH_HEADER_NAME = "Authorization"
}
