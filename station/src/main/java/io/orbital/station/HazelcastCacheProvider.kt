package io.orbital.station

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.hazelcast.HazelcastConnectionsManager
import com.orbitalhq.connectors.hazelcast.HazelcastOperationCacheBuilder
import com.orbitalhq.connectors.hazelcast.HazelcastStateStoreProvider
import com.orbitalhq.schema.consumer.SchemaStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HazelcastOperationCacheConfig {

   @Bean
   fun hazelcastConnectionsManager(connectors: SourceLoaderConnectorsRegistry): HazelcastConnectionsManager {
      return HazelcastConnectionsManager(connectors)

   }
   @Bean
   fun hazelcastOperationCacheProviderBuilder(hazelcastConnectionsManager: HazelcastConnectionsManager, schemaStore: SchemaStore): HazelcastOperationCacheBuilder {
      return HazelcastOperationCacheBuilder(hazelcastConnectionsManager, schemaStore)
   }

   @Bean
   fun hazelcastStateStoreProvider(hazelcastConnectionsManager: HazelcastConnectionsManager): HazelcastStateStoreProvider {
      return HazelcastStateStoreProvider(hazelcastConnectionsManager)
   }
}
