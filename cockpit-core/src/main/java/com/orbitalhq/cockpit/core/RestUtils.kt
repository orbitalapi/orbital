package com.orbitalhq.cockpit.core

import com.orbitalhq.spring.http.VyneQueryServiceExceptionProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VyneQueryServiceExceptionProviderConfig {
   @Bean
   fun exceptionProvider() = VyneQueryServiceExceptionProvider()
}
