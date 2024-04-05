package com.orbitalhq.cockpit.core

import com.orbitalhq.spring.rsocket.RSocketConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies

@Configuration
class RSocketConfig {

   @Bean
   fun rsocketFactory(strategies: RSocketStrategies) = RSocketConnectionFactory(strategies)
}
