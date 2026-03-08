package com.orbitalhq.connectors.redis

import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.redis.RedisConfiguration
import io.lettuce.core.api.StatefulRedisConnection
import java.util.concurrent.ConcurrentHashMap

object RedisConnections {
   const val QUERY_CACHE = "_query"
}

class RedisConnectionsManager(
   private val connectors: SourceLoaderConnectorsRegistry
) : RedisConnectionProvider {

   private val redisConnections = ConcurrentHashMap<RedisConfiguration, StatefulRedisConnection<String, String>>()

   override fun redisConnection(connectionName: String?): Pair<StatefulRedisConnection<String, String>, RedisConfiguration> {
      return if (connectionName == null) {
         val defaultRedisConnection = connectors.defaultRedisConfiguration()
            ?: error("Cannot fetch Redis connection, as no connection name was provided, and there are no default Redis connections configured.")
         val connection = redisConnections.getOrPut(defaultRedisConnection) {
            RedisConnectionFactory.createConnection(defaultRedisConnection)
         }
         connection to defaultRedisConnection
      } else {
         val connectionConfig = connectors.redisConfigurationForConnectionName(connectionName)
         require(connectionConfig != null) { "No connection for Redis named $connectionName exists" }
         val connection = redisConnections.getOrPut(connectionConfig) {
            RedisConnectionFactory.createConnection(connectionConfig)
         }
         connection to connectionConfig
      }
   }

   override fun canProvideRedisConnection(connectionName: String?): Boolean {
      return (connectionName == null && connectors.defaultRedisConfiguration() != null) ||
         (connectionName != null && connectors.redisConfigurationForConnectionName(connectionName) != null)
   }

   internal fun connectionsCount() = redisConnections.size

   override fun provide(config: RedisConfiguration): StatefulRedisConnection<String, String> {
      return redisConnection(config.connectionName).first
   }

   fun close() {
      redisConnections.values.forEach { it.close() }
      redisConnections.clear()
   }
}
