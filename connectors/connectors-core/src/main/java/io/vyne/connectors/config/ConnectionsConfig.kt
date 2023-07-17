package io.vyne.connectors.config

import io.vyne.connectors.config.gcp.GcpConnectionConfiguration
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionsConfig(
   // jdbc was a map to an interface, but that makes serde to/from HOCON really hard.
   override val jdbc: Map<String, DefaultJdbcConnectionConfiguration> = emptyMap(),
   override val kafka: Map<String, KafkaConnectionConfiguration> = emptyMap(),
   override val googleCloud: Map<String, GcpConnectionConfiguration> = emptyMap()
) : IConnectionsConfig {
   val jdbcConnectionsHash = jdbc.hashCode()
   val kafkaConnectionsHash = kafka.hashCode()

   companion object {
      fun empty(): ConnectionsConfig {
         return ConnectionsConfig()
      }
   }
}


// Really, just for testing, so we can have a mutable variant
interface IConnectionsConfig {
   val jdbc: Map<String, DefaultJdbcConnectionConfiguration>
   val kafka: Map<String, KafkaConnectionConfiguration>
   val googleCloud: Map<String, GcpConnectionConfiguration>
}

// For testing
data class MutableConnectionsConfig(
   override val jdbc: MutableMap<String, DefaultJdbcConnectionConfiguration> = mutableMapOf(),
   override val kafka: MutableMap<String, KafkaConnectionConfiguration> = mutableMapOf(),
   override val googleCloud: MutableMap<String, GcpConnectionConfiguration> = mutableMapOf()
) : IConnectionsConfig
