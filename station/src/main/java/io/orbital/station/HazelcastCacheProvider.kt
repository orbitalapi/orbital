package io.orbital.station

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.hazelcast.HazelcastOperationCacheBuilder
import com.orbitalhq.connectors.hazelcast.HazelcastStateStoreProvider
import com.orbitalhq.schema.consumer.SchemaStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HazelcastOperationCacheConfig {

   @Bean
   fun hazelcastOperationCacheProviderBuilder(connectors: SourceLoaderConnectorsRegistry,schemaStore: SchemaStore): HazelcastOperationCacheBuilder {
      return HazelcastOperationCacheBuilder(connectors, schemaStore)
   }

   @Bean
   fun hazlecastStateStoreProvider(connectors: SourceLoaderConnectorsRegistry): HazelcastStateStoreProvider {
      return HazelcastStateStoreProvider(connectors)
   }
}
