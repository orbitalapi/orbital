package io.vyne.connectors.config

import io.vyne.connectors.config.aws.AwsConnectionConfiguration
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
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
   val aws: Map<String, AwsConnectionConfiguration> = emptyMap()
) {
   val jdbcConnectionsHash = jdbc.hashCode()
   val kafkaConnectionsHash = kafka.hashCode()

   companion object {
      fun empty(): ConnectionsConfig {
         return ConnectionsConfig()
      }
   }
}
