package com.orbitalhq.connectors.config.kafka

import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorType
import com.orbitalhq.utils.obfuscateKeys
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
   val connectionParameters: Map<ConnectionParameterName, String>,
) : ConnectorConfiguration {
   override val type: ConnectorType = ConnectorType.MESSAGE_BROKER
   override fun getUiDisplayProperties(): Map<String, Any> {
      return connectionParameters
         .obfuscateKeys(
            "sasl.oauthbearer.client.secret",
            "sasl.oauthbearer.client.id",
            "sasl.oauthbearer.refresh.token",

            // SSL/TLS Settings
            "ssl.keystore.password",
            "ssl.key.password",
            "ssl.truststore.password",

            // Confluent Schema Registry (if using basic authentication)
            "basic.auth.user.info", // may contain credentials
            "schema.registry.ssl.truststore.password",
            "schema.registry.ssl.keystore.password",
            "schema.registry.basic.auth.user.info"
         )
         .obfuscateKeys(listOf("sasl.jaas.config")) { _, value ->
            // sasl.jaas.config is provided as follows:
            // sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=freddy password=i_am_the_user
            // So, we need replace a subsection of the string
            val passwordPattern = "password=[^;]*".toRegex()
            value.replace(passwordPattern, "password=******")
         }
   }

   override val driverName: String = KafkaConnection.DRIVER_NAME

   constructor(
      connectionName: String,
      brokerAddress: String,
      groupId: String,
      connectionParameters: Map<ConnectionParameterName, String> = emptyMap()
   ) : this(
      connectionName,
      connectionParameters +
      mapOf(
         KafkaConnection.Parameters.BROKERS.templateParamName to brokerAddress,
         KafkaConnection.Parameters.GROUP_ID.templateParamName to groupId
      )
   )


}
