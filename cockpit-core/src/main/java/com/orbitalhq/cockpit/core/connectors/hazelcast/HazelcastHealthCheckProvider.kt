package com.orbitalhq.cockpit.core.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.cockpit.core.connectors.ConnectionHealthProvider
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.config.hazelcast.HazelcastConnection
import com.orbitalhq.connectors.hazelcast.HazelcastInstanceProvider
import com.orbitalhq.connectors.hazelcast.doHealthCheck
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class HazelcastHealthCheckProvider(private val hazelcastInstanceProvider: HazelcastInstanceProvider): ConnectionHealthProvider {
   private val hazelcastClientMap = ConcurrentHashMap<String, HazelcastInstance>()
   override fun canProvideFor(config: ConnectorConfiguration): Boolean {
      return config.type == ConnectorType.CACHE && config.driverName == HazelcastConnection.DRIVER_NAME
   }

   override fun provide(config: ConnectorConfiguration): Mono<ConnectionStatus> {
      require(config is HazelcastConfiguration) { "Expected to receive a ${HazelcastConfiguration::class.simpleName}, but got ${config::class.simpleName}"}
      return Mono.create { sink ->
         val result = try {
            hazelcastClientMap.getOrPut(config.connectionName) {
               hazelcastInstanceProvider.provide(config)
            }.doHealthCheck()
            ConnectionStatus.ok()
         } catch (e:Exception) {
            ConnectionStatus.error(e.message ?: e::class.simpleName!!)
         }
         sink.success(result)
      }
   }
}
