package com.orbitalhq.cockpit.core.security.authentication

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@ConditionalOnProperty("vyne.security.openIdp.enabled", havingValue = "false", matchIfMissing = true)
@Configuration
class UnsecureConfig {
   @Bean
   fun springWebFilterChainNoAuthentication(http: ServerHttpSecurity): SecurityWebFilterChain? {
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
