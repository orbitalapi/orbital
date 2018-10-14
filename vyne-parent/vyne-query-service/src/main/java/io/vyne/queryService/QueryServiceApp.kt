package io.vyne.queryService

import io.vyne.spring.EnableVyne
import io.vyne.spring.RemoteSchemaStoreType
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@SpringBootApplication
@EnableEurekaClient
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
class QueryServiceApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(QueryServiceApp::class.java, *args)
      }
   }

    @Configuration
    //   @EnableWebMvc
    class WebConfig : WebMvcConfigurerAdapter() {

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
                .allowedHeaders(AuthHeaders.AUTH_HEADER_NAME)
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
