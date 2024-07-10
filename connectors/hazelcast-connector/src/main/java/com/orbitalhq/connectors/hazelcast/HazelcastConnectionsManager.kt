package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfigurationType
import com.orbitalhq.schema.consumer.SchemaStore
import java.util.concurrent.ConcurrentHashMap

object HazelcastConnections {
   const val QUERY_CACHE = "_query"
}

class HazelcastConnectionsManager(private val connectors: SourceLoaderConnectorsRegistry, private val schemaStore: SchemaStore, private val orbitalHazelcastInstance: HazelcastInstance) : HazelcastInstanceProvider {
   private val hazelcastConnections = ConcurrentHashMap<HazelcastConfiguration, HazelcastInstance>()

   override fun hazelcastConnection(connectionName: String?): Pair<HazelcastInstance, HazelcastConfiguration> {
      return if (connectionName == null) {
         val defaultHazelcastConnection = connectors.defaultHazelcastConfiguration()
         require(defaultHazelcastConnection != null)
         val hzInstance = hazelcastConnections.getOrPut(defaultHazelcastConnection) {
            if (defaultHazelcastConnection.configType == HazelcastConfigurationType.Embedded) {
               orbitalHazelcastInstance
            } else {
               HazelcastBuilder.build(
                  defaultHazelcastConnection,
                  HazelcastConnections.QUERY_CACHE,
                  schemaStore
               )
            }
         }
         hzInstance to defaultHazelcastConnection
      } else {
         val connectionConfig = connectors.hazelcastConfigurationForConnectionName(connectionName)
         require(connectionConfig != null) { "No connection for Hazelcast named $connectionName exists" }
         val hzInstance = hazelcastConnections.getOrPut(connectionConfig) {
            if (connectionConfig.configType == HazelcastConfigurationType.Embedded) {
               orbitalHazelcastInstance
            } else {
               HazelcastBuilder.build(connectionConfig, HazelcastConnections.QUERY_CACHE, schemaStore)
            }
         }
         hzInstance to connectionConfig
      }
   }

   override fun canProvideHazelcastInstance(connectionName: String?): Boolean {
      return (connectionName == null && connectors.defaultHazelcastConfiguration() != null) ||
         connectionName != null && connectors.hazelcastConfigurationForConnectionName(connectionName) != null

   }

   override fun provide(config: HazelcastConfiguration): HazelcastInstance {
      return hazelcastConnection(config.connectionName).first
   }
}
