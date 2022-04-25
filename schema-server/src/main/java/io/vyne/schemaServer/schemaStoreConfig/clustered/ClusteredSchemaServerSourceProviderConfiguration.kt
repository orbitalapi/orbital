package io.vyne.schemaServer.schemaStoreConfig.clustered

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.vyne.schema.publisher.http.HttpPollKeepAliveStrategyMonitor
import io.vyne.schema.publisher.http.HttpPollKeepAliveStrategyPollUrlResolver
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schemaServer.schemaStoreConfig.SchemaUpdateNotifier
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${vyne.schema.publicationMethod:}') && \${vyne.schema.server.clustered:false}")
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
      @Value("\${vyne.schema.management.ttlCheckInSeconds:1}") ttlCheckInSeconds: Long,
      @Value("\${vyne.schema.management.httpRequestTimeoutInSeconds:30}") httpRequestTimeoutInSeconds: Long,
      httpPollKeepAliveStrategyPollUrlResolver: HttpPollKeepAliveStrategyPollUrlResolver,
      webClientBuilder: WebClient.Builder,
      hazelcastInstance: HazelcastInstance
   ): HttpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(
      ttlCheckPeriod = Duration.ofSeconds(ttlCheckInSeconds),
      httpRequestTimeoutInSeconds = httpRequestTimeoutInSeconds,
      pollUrlResolver = httpPollKeepAliveStrategyPollUrlResolver,
      webClientBuilder = webClientBuilder,
      lastPingTimes = hazelcastInstance.getMap("httpPollKeepAliveStrategyMonitorPingMap"))

   @Bean
   @ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
   fun expiringSourcesStore(hazelcastInstance: HazelcastInstance, keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>): ExpiringSourcesStore {
     return ExpiringSourcesStore(keepAliveStrategyMonitors = keepAliveStrategyMonitors, sources = hazelcastInstance.getMap("expiringSourceMap"))
   }

   @Bean
   @ConditionalOnExpression("\${vyne.schema.server.clustered:false}")
   fun schemaUpdateNotifier(validatingSchemaStoreClient: ValidatingSchemaStoreClient, hazelcastInstance: HazelcastInstance): SchemaUpdateNotifier {
      return DistributedSchemaUpdateNotifier(hazelcastInstance.getReliableTopic("/vyne/schemaUpdate"), validatingSchemaStoreClient )
   }

   @Bean
   fun hazelcastInstance() = Hazelcast.newHazelcastInstance()
}
