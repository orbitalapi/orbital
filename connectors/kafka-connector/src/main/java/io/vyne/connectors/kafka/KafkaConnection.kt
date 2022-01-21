package io.vyne.connectors.kafka

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.kafka.builders.KafkaConnectionBuilder
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType

object KafkaConnection {
   val driverOptions = ConnectionDriverOptions(
      "kafka", "Kafka", KafkaConnectionBuilder.parameters
   )
}

/**
 * Represents a persistable kafka connection with parameters.
 * This should be used to create an actual connection to kafka
 */
data class DefaultKafkaConnectionConfiguration(
   override val connectionName: String,
   // connectionParameters must be typed as Map<String,String> (rather than <String,Any>
   // as the Hocon persistence library we're using can't deserialize values from disk into
   // an Any.  If this causes issues, we'll need to wrap the deserialization to coerce numbers from strings.
   val connectionParameters: Map<ConnectionParameterName, String>,

   ) : KafkaConnectionConfiguration {
   companion object {
      fun forParams(
         connectionName: String,
         connectionParameters: Map<IConnectionParameter, String>
      ): DefaultKafkaConnectionConfiguration {
         return DefaultKafkaConnectionConfiguration(
            connectionName,
            connectionParameters.mapKeys { it.key.templateParamName }
         )
      }
   }

}

interface KafkaConnectionConfiguration : ConnectorConfiguration {
   override val driverName: String
      get() = "kafka"

   override val address: String
      get() = "kafka"

   override val connectionName: String

   override val type: ConnectorType
      get() = ConnectorType.KAFKA
}

