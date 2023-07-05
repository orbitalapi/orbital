package io.vyne.query.runtime.executor.analytics

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.ResultRowPersistenceStrategyFactory
import io.vyne.history.codec.VyneHistoryRecordDecoder
import io.vyne.history.codec.VyneHistoryRecordObjectEncoder
import io.vyne.history.remote.RemoteQueryEventConsumerClient
import io.vyne.models.json.Jackson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import java.util.concurrent.Executors

@Configuration
class AnalyticsSpringConfig {

   private val historyDispatcher = Executors
      .newFixedThreadPool(1)
      .asCoroutineDispatcher()

   @Bean
   fun rsocketStrategies(): RSocketStrategies = RSocketStrategies.builder()
      .encoders { it.add(VyneHistoryRecordObjectEncoder()) }
      .decoders { it.add(VyneHistoryRecordDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()


   @Bean
   fun queryEventConsumerClient(queryAnalyticsConfig: QueryAnalyticsConfig): RemoteQueryEventConsumerClient {
      return RemoteQueryEventConsumerClient(
         ResultRowPersistenceStrategyFactory.resultRowPersistenceStrategy(
            Jackson.defaultObjectMapper,
            null,
            queryAnalyticsConfig
         ),
         config = queryAnalyticsConfig,
         CoroutineScope(historyDispatcher)
      )
   }
}
