package io.vyne.historyServer.server

import io.vyne.history.codec.VyneHistoryRecordDecoder
import io.vyne.history.codec.VyneHistoryRecordObjectEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.web.util.pattern.PathPatternRouteMatcher

@Configuration
class RSocketServerConfiguration {
   @Bean
   fun rsocketMessageHandler(rsocketStrategies: RSocketStrategies) = RSocketMessageHandler().apply {
      rSocketStrategies = rsocketStrategies
   }

   @Bean
   fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(VyneHistoryRecordObjectEncoder()) }
      .decoders { it.add(VyneHistoryRecordDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

}
