package io.vyne.queryService;

import io.polymer.spring.EnablePolymer;
import io.polymer.spring.RemoteSchemaStoreType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
//@EnableEurekaClient
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
public class QueryServiceApp {

   public static void main(String[] args) {
      SpringApplication.run(QueryServiceApp.class, args);
   }

   @Configuration
   @EnableWebMvc
   public static class WebConfig extends WebMvcConfigurerAdapter {

      @Override
      public void addCorsMappings(CorsRegistry registry) {
         registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("PUT", "DELETE","GET","POST")
//            .allowedHeaders("header1", "header2", "header3")
//            .exposedHeaders("header1", "header2")
            .allowCredentials(false).maxAge(3600);
      }
   }
}
