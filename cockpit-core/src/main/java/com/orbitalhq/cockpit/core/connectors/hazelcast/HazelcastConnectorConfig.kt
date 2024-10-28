package com.orbitalhq.cockpit.core.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.hazelcast.HazelcastConnectionsManager
import com.orbitalhq.connectors.hazelcast.HazelcastOperationCacheBuilder
import com.orbitalhq.connectors.hazelcast.HazelcastStateStoreProvider
import com.orbitalhq.connectors.hazelcast.invoker.HazelcastInvoker
import com.orbitalhq.schema.consumer.SchemaStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HazelcastConnectorConfig {
   @Bean
   fun hazelcastConnectionsManager(connectors: SourceLoaderConnectorsRegistry, orbitalHazelcastInstance: HazelcastInstance): HazelcastConnectionsManager {
      return HazelcastConnectionsManager(connectors, orbitalHazelcastInstance)

   }
   @Bean
   fun hazelcastOperationCacheProviderBuilder(hazelcastConnectionsManager: HazelcastConnectionsManager, schemaStore: SchemaStore): HazelcastOperationCacheBuilder {
      return HazelcastOperationCacheBuilder(hazelcastConnectionsManager, schemaStore)
   }

   @Bean
   fun hazelcastStateStoreProvider(hazelcastConnectionsManager: HazelcastConnectionsManager): HazelcastStateStoreProvider {
      return HazelcastStateStoreProvider(hazelcastConnectionsManager)
   }

   @Bean
   fun hazelcastInvoker(connectionsManager: HazelcastConnectionsManager): HazelcastInvoker {
      return HazelcastInvoker(connectionsManager)
   }

}
