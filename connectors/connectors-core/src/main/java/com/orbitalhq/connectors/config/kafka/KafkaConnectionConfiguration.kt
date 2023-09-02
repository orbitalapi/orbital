package com.orbitalhq.connectors.config.kafka

import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import kotlinx.serialization.Serializable

/**
 * Represents a persistable kafka connection with parameters.
 * This should be used to create an actual connection to kafka
 */
@Serializable
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
