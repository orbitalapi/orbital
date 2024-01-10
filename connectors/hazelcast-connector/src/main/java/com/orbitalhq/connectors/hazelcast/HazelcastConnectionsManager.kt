package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import java.util.concurrent.ConcurrentHashMap

object HazelcastConnections {
   const val QUERY_CACHE = "_query"
}

class HazelcastConnectionsManager(private val connectors: SourceLoaderConnectorsRegistry) : HazelcastInstanceProvider {
   private val hazelcastConnections = ConcurrentHashMap<HazelcastConfiguration, HazelcastInstance>()

   fun hazelcastConnection(connectionName: String?): Pair<HazelcastInstance, HazelcastConfiguration> {
      return if (connectionName == null) {
         val defaultHazelcastConnection = connectors.defaultHazelcastConfiguration()
         require(defaultHazelcastConnection != null)
         val hzInstance = hazelcastConnections.getOrPut(defaultHazelcastConnection) {
            HazelcastBuilder.build(
               defaultHazelcastConnection,
               HazelcastConnections.QUERY_CACHE
            )
         }
         hzInstance to defaultHazelcastConnection
      } else {
         val connectionConfig = connectors.hazelcastConfigurationForConnectionName(connectionName)
         require(connectionConfig != null) { "No connection for Hazelcast named $connectionName exists" }
         val hzInstance = hazelcastConnections.getOrPut(connectionConfig) {
            HazelcastBuilder.build(connectionConfig, HazelcastConnections.QUERY_CACHE)
         }
         hzInstance to connectionConfig
      }
   }

   fun canProvideHazelcastInstance(connectionName: String?): Boolean {
      return (connectionName == null && connectors.defaultHazelcastConfiguration() != null) ||
         connectionName != null && connectors.hazelcastConfigurationForConnectionName(connectionName) != null

   }

   override fun provide(config: HazelcastConfiguration): HazelcastInstance {
      return hazelcastConnection(config.connectionName).first
   }
}
