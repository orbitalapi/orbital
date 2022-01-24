package io.vyne.connectors.kafka

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.connectionParams
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType


object KafkaConnection {
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      BROKERS(ConnectionDriverParam("Broker address", SimpleDataType.STRING, templateParamName = "brokerAddress")),
      GROUP_ID(
         ConnectionDriverParam(
            "Group Id",
            SimpleDataType.STRING,
            defaultValue = "vyne",
            templateParamName = "groupId",
         )
      ),
   }

   const val DRIVER_NAME = "KAFKA"

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
   val driverOptions = ConnectionDriverOptions(
      "KAFKA", "Kafka", ConnectorType.MESSAGE_BROKER, parameters
   )
}

/**
 * Represents a persistable kafka connection with parameters.
 * This should be used to create an actual connection to kafka
 */
data class KafkaConnectionConfiguration(
   override val connectionName: String,
   // connectionParameters must be typed as Map<String,String> (rather than <String,Any>
   // as the Hocon persistence library we're using can't deserialize values from disk into
   // an Any.  If this causes issues, we'll need to wrap the deserialization to coerce numbers from strings.
   val connectionParameters: Map<ConnectionParameterName, String>
) : ConnectorConfiguration {
   override val type: ConnectorType = ConnectorType.MESSAGE_BROKER
   override val driverName: String = KafkaConnection.DRIVER_NAME

   constructor(
      connectionName: String,
      brokerAddress: String,
      groupId: String
   ) : this(
      connectionName,
      mapOf(
         KafkaConnection.Parameters.BROKERS.templateParamName to brokerAddress,
         KafkaConnection.Parameters.GROUP_ID.templateParamName to groupId
      )
   )

}

// Using extension functions to avoid serialization issues with HOCON
val KafkaConnectionConfiguration.brokers: String
   get() {
      return this.connectionParameters[KafkaConnection.Parameters.BROKERS.templateParamName] as String
   }
val KafkaConnectionConfiguration.groupId: String
   get() {
      return this.connectionParameters[KafkaConnection.Parameters.GROUP_ID.templateParamName] as String
   }
