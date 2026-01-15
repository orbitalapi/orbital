package com.orbitalhq.cockpit.core.connectors.redis

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.redis.RedisConnectionsManager
import com.orbitalhq.connectors.redis.RedisOperationCacheBuilder
import com.orbitalhq.connectors.redis.RedisStateStoreProvider
import com.orbitalhq.connectors.redis.invoker.RedisInvoker
import com.orbitalhq.schema.consumer.SchemaStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConnectorConfig {

   @Bean
   fun redisConnectionsManager(connectors: SourceLoaderConnectorsRegistry): RedisConnectionsManager {
      return RedisConnectionsManager(connectors)
   }

   @Bean
   fun redisOperationCacheBuilder(
      redisConnectionsManager: RedisConnectionsManager,
      schemaStore: SchemaStore
   ): RedisOperationCacheBuilder {
      return RedisOperationCacheBuilder(redisConnectionsManager, schemaStore)
   }

   @Bean
   fun redisStateStoreProvider(redisConnectionsManager: RedisConnectionsManager): RedisStateStoreProvider {
      return RedisStateStoreProvider(redisConnectionsManager)
   }

   @Bean
   fun redisInvoker(connectionsManager: RedisConnectionsManager): RedisInvoker {
      return RedisInvoker(connectionsManager)
   }
}
