package io.vyne.queryService

import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.api.QueryHistoryServiceRestApi
import io.vyne.history.codec.VyneHistoryRecordDecoder
import io.vyne.history.codec.VyneHistoryRecordObjectEncoder
import io.vyne.history.proxy.QueryHistoryServiceRestProxy
import io.vyne.history.remote.QueryHistoryRemoteWriter
import io.vyne.query.HistoryEventConsumerProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactivefeign.spring.config.EnableReactiveFeignClients

private val logger = KotlinLogging.logger {}
/**
 * Activated when Query history is pushed to a remote server for persistence.
 */
@ConditionalOnProperty(prefix = "vyne.analytics", name = ["mode"], havingValue = "Remote", matchIfMissing = false)
@Configuration
@EnableReactiveFeignClients(clients = [QueryHistoryServiceRestApi::class])
class RemoteHistoryConfig {
   @Bean
   fun historyWriterProvider(
      config: QueryAnalyticsConfig,
      rsocketStrategies: RSocketStrategies,
      discoveryClient: DiscoveryClient): HistoryEventConsumerProvider {
      logger.info { "Analytics data will stored by remote analytics server" }
      return QueryHistoryRemoteWriter(config, rsocketStrategies, discoveryClient)
   }

   @Bean
   fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(VyneHistoryRecordObjectEncoder()) }
      .decoders { it.add(VyneHistoryRecordDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

   @Bean
   fun  queryHistoryServiceRestProxy(
      feignClient: QueryHistoryServiceRestApi,
      @Value("\${vyne.queryAnalyticsService.name:vyne-analytics-server}") historyServer: String,
      discoveryClient: DiscoveryClient) = QueryHistoryServiceRestProxy(feignClient, historyServer, discoveryClient)
}
