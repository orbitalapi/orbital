package io.orbital.station

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.hazelcast.HazelcastCacheProviderBuilder
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.schema.consumer.SchemaStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class HazelcastOperationCacheConfig {

   @Bean
   fun hazelcastOperationCacheProviderBuilder(connectors: SourceLoaderConnectorsRegistry,schemaStore: SchemaStore): HazelcastCacheProviderBuilder {
      return HazelcastCacheProviderBuilder(connectors, schemaStore)
   }
}
