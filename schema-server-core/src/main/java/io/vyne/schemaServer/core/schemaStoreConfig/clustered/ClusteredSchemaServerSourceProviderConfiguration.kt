package io.vyne.schemaServer.core.schemaStoreConfig.clustered

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schema.publisher.http.HttpPollKeepAliveStrategyMonitor
import io.vyne.schemaServer.core.config.SchemaUpdateNotifier
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@ConditionalOnProperty("vyne.schema.server.clustered", havingValue = "true", matchIfMissing = false)
@Configuration
class ClusteredSchemaServerSourceProviderConfiguration {
   @Bean
   @ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
   fun localValidatingSchemaStoreClient(hazelcastInstance: HazelcastInstance): ValidatingSchemaStoreClient {
      return DistributedSchemaStoreClient(hazelcastInstance)
   }

   @Bean
   @ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
   fun httpPollKeepAliveStrategyMonitor(
      @Value("\${vyne.schema.management.keepAlivePollFrequency:1s}") keepAlivePollFrequency: Duration,
      @Value("\${vyne.schema.management.httpRequestTimeout:30s}") httpRequestTimeout: Duration,
      webClientBuilder: WebClient.Builder,
      hazelcastInstance: HazelcastInstance
   ): HttpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(
      pollFrequency = keepAlivePollFrequency,
      httpRequestTimeout = httpRequestTimeout,
      webClientBuilder = webClientBuilder,
      lastPingTimes = hazelcastInstance.getMap("httpPollKeepAliveStrategyMonitorPingMap")
   )

   @Bean
   @ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
   fun expiringSourcesStore(
      hazelcastInstance: HazelcastInstance,
      keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>
   ): ExpiringSourcesStore {
      return ExpiringSourcesStore(
         keepAliveStrategyMonitors = keepAliveStrategyMonitors,
         packages = hazelcastInstance.getMap("expiringSourceMap")
      )
   }

   @Bean
   @ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
   fun schemaUpdateNotifier(
      validatingSchemaStoreClient: ValidatingSchemaStoreClient,
      hazelcastInstance: HazelcastInstance
   ): SchemaUpdateNotifier {
      return DistributedSchemaUpdateNotifier(
         hazelcastInstance.getReliableTopic("/vyne/schemaUpdate"),
         validatingSchemaStoreClient
      )
   }

   @Bean
   fun hazelcastInstance() = Hazelcast.newHazelcastInstance()
}
