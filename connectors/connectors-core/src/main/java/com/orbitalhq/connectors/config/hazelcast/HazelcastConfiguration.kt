package com.orbitalhq.connectors.config.hazelcast

import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
class HazelcastConfiguration(
   override val connectionName: String,
   val addresses: List<String>,
   val operationCacheTtlSeconds: Int = 120
) : ConnectorConfiguration {

   override val driverName: String = HazelcastConnection.DRIVER_NAME
   override val type: ConnectorType = ConnectorType.CACHE
   override fun getUiDisplayProperties(): Map<String, Any> = mapOf("addresses" to addresses.joinToString(","))

}

object HazelcastConnection {
   const val DRIVER_NAME = "hazelcast"
}
