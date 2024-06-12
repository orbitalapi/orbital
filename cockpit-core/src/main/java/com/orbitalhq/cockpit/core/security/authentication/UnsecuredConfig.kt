package com.orbitalhq.cockpit.core.security.authentication

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@ConditionalOnExpression("\${vyne.security.openIdp.enabled:false} == false and \${vyne.security.saml.enabled:false} == false")
@Configuration
class UnsecureConfig {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   @Bean
   fun springWebFilterChainNoAuthentication(http: ServerHttpSecurity): SecurityWebFilterChain? {
      logger.warn { "Authentication is disabled" }
      return http
         .csrf().disable()
         .cors().disable()
         .headers().disable()
         .authorizeExchange()
         .anyExchange().permitAll()
         .and()
         .build()
   }
}
