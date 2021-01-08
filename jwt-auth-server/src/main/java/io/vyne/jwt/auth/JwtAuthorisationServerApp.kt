package io.vyne.jwt.auth

import io.vyne.jwt.auth.config.KeycloakServerProperties
import io.vyne.jwt.auth.config.log
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean

@SpringBootApplication(exclude = [LiquibaseAutoConfiguration::class])
@EnableConfigurationProperties(KeycloakServerProperties::class)
class JwtAuthorisationServerApp {
   @Bean
   fun onApplicationReadyEventListener(serverProperties: ServerProperties, keycloakServerProperties: KeycloakServerProperties): ApplicationListener<ApplicationReadyEvent?>? {
      return ApplicationListener { evt: ApplicationReadyEvent? ->
         val port = serverProperties.port
         val keycloakContextPath: String = keycloakServerProperties.contextPath
         log().info("Embedded Keycloak started: http://localhost:$port$keycloakContextPath to use keycloak")
      }
   }
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(JwtAuthorisationServerApp::class.java)
         app.run(*args)
      }
   }
}
