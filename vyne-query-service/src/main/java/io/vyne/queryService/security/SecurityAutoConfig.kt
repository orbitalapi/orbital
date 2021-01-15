package io.vyne.queryService.security

import io.vyne.queryService.schemas.VyneQueryBuiltInTypesProvider
import io.vyne.schemaStore.SchemaPublisher
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Profile("secure")
@Configuration
@EnableWebSecurity
@Import(value = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class, AuthenticationToVyneUserConverter::class])
class SecurityAutoConfig : WebSecurityConfigurerAdapter() {
   override fun configure(http: HttpSecurity) {
      http
         .csrf().ignoringAntMatchers("/eureka/**").and()
         .cors()
         .and()
         .authorizeRequests()
         .antMatchers(
            "/api/security/config",
            "/api/schemas"
         ).permitAll() // End point for Cask and other vyne based services to fetch the schema in EUREKA schema discovery mode.
         .antMatchers("/api/**").authenticated() // All other api endpoints must be authenticated
         .antMatchers(
            "/**", // Allow access to any, to support html5 ui routes (eg /types/foo.bar.Baz)
            "/eureka/**",
            "/eureka/",
            "/eureka",
            "/assets/**",
            "/index.html",
            "/*.js",
            "/*.css"
         ).permitAll()
         .anyRequest()
         .authenticated()
         .and()
         // Below sets up the Vyne as a resource server, so Vyne will check if there is an 'access' token on every request
         // and whether the token is valid or not. In order to verify that a token is genuine, it'll talk to openID connect server
         // (see spring.security.oauth2.resourceserver.jwt.jwk-set-uri)
         .oauth2ResourceServer()
         .jwt()
   }

   @Bean
   fun onApplicationReadyEventListener(schemaPublisher: SchemaPublisher): ApplicationListener<ApplicationReadyEvent?>? {
      return ApplicationListener { evt: ApplicationReadyEvent? ->
         schemaPublisher.submitSchemas(VyneQueryBuiltInTypesProvider.versionedSources)
      }
   }
}
