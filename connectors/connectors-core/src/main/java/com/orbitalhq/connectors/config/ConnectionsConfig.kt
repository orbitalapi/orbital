package com.orbitalhq.connectors.config

import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import kotlinx.serialization.Serializable

/**
 * Single config file (which matches the HOCON serialization structure)
 * that describes all connections.
 *
 * This is the preferred method for documenting connections, as it's supported
 * for Schema loading, and is serializable for sending to query nodes.
 *
 * Other mechanisms (where we had a loader-per-connector) are deprecated in
 * favour of this approach.
 */
@Serializable
data class ConnectionsConfig(
   // jdbc was a map to an interface, but that makes serde to/from HOCON really hard.
   val jdbc: Map<String, DefaultJdbcConnectionConfiguration> = emptyMap(),
   val kafka: Map<String, KafkaConnectionConfiguration> = emptyMap(),
   val aws: Map<String, AwsConnectionConfiguration> = emptyMap(),
   val hazelcast: Map<String,HazelcastConfiguration> = emptyMap()
) {
   fun listAll(): List<ConnectorConfiguration> {
      return jdbc.values + kafka.values + aws.values + hazelcast.values
   }

   val jdbcConnectionsHash = jdbc.hashCode()
   val kafkaConnectionsHash = kafka.hashCode()

   companion object {
      fun empty(): ConnectionsConfig {
         return ConnectionsConfig()
      }
   }
}
