package io.vyne.queryService

import io.vyne.history.QueryHistoryConfig
import io.vyne.history.api.QueryHistoryServiceRestApi
import io.vyne.history.codec.VyneHistoryRecordDecoder
import io.vyne.history.codec.VyneHistoryRecordObjectEncoder
import io.vyne.history.proxy.QueryHistoryServiceRestProxy
import io.vyne.history.remote.QueryHistoryRemoteWriter
import org.springframework.beans.factory.annotation.Value
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
@ConditionalOnProperty(prefix = "vyne.history", name = ["mode"], havingValue = "Remote", matchIfMissing = false)
@Configuration
@EnableReactiveFeignClients(clients = [QueryHistoryServiceRestApi::class])
class RemoteHistoryConfig {
   @Bean
   fun historyWriterProvider(
      config: QueryHistoryConfig,
      rsocketStrategies: RSocketStrategies,
      discoveryClient: DiscoveryClient) = QueryHistoryRemoteWriter(config, rsocketStrategies, discoveryClient)

   @Bean
   fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(VyneHistoryRecordObjectEncoder()) }
      .decoders { it.add(VyneHistoryRecordDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()

   @Bean
   fun  queryHistoryServiceRestProxy(
      feignClient: QueryHistoryServiceRestApi,
      @Value("\${vyne.queryHistoryService.name:vyne-history-server}") historyServer: String,
      discoveryClient: DiscoveryClient) = QueryHistoryServiceRestProxy(feignClient, historyServer, discoveryClient)
}
