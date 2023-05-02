package io.vyne.connectors.config

import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import kotlinx.serialization.Serializable

@Serializable
data class ConnectorsConfig(
   // jdbc was a map to an interface, but that makes serde to/from HOCON really hard.
   val jdbc: Map<String,DefaultJdbcConnectionConfiguration> = emptyMap(),
   val kafka: Map<String, KafkaConnectionConfiguration> = emptyMap()
) {
   val jdbcConnectionsHash = jdbc.hashCode()
   val kafkaConnectionsHash = kafka.hashCode()

   companion object {
      fun empty(): ConnectorsConfig {
         return ConnectorsConfig()
      }
   }
}
