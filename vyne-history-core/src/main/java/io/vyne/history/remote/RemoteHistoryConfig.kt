package io.vyne.history.remote

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.api.QueryHistoryServiceRestApi
import io.vyne.history.codec.VyneHistoryRecordDecoder
import io.vyne.history.codec.VyneHistoryRecordObjectEncoder
import io.vyne.query.HistoryEventConsumerProvider
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactivefeign.spring.config.EnableReactiveFeignClients

/**
 * Activated when Query history is pushed to a remote server for persistence.
 */
@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "Remote", matchIfMissing = false)
@Configuration
@EnableReactiveFeignClients(clients = [QueryHistoryServiceRestApi::class])
class RemoteHistoryConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @Bean
   fun historyWriterProvider(
      config: QueryAnalyticsConfig,
      rsocketStrategies: RSocketStrategies,
      discoveryClient: DiscoveryClient
   ): HistoryEventConsumerProvider {
      logger.info { "Analytics data will sent to a remote analytics server" }
      return QueryHistoryRSocketWriter(config, rsocketStrategies, discoveryClient)
   }

   @Bean
   fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(VyneHistoryRecordObjectEncoder()) }
      .decoders { it.add(VyneHistoryRecordDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

   // TODO : This was used when running with a seperate analytics server.
   // We definitely still need this, but it's unclear right now where this should sit.
   // It will become clearer as our target architecture firms up.
//   @Bean
//   fun  queryHistoryServiceRestProxy(
//       feignClient: QueryHistoryServiceRestApi,
//       @Value("\${vyne.queryAnalyticsService.name:vyne-analytics-server}") historyServer: String,
//       discoveryClient: DiscoveryClient
//   ) = QueryHistoryServiceRestProxy(feignClient, historyServer, discoveryClient)
}
