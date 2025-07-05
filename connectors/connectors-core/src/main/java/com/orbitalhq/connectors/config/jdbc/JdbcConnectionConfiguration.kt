package com.orbitalhq.connectors.config.jdbc

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorCategory
import com.orbitalhq.utils.obfuscateKeys
import kotlinx.serialization.Serializable


/**
 * Represents a persistable jdbc connection with parameters.
 * This should be used to create an actual connection to the db
 */
@Serializable
data class DefaultJdbcConnectionConfiguration(
   override val connectionName: String,
   override val jdbcDriver: DatabaseDriverName,
   // connectionParameters must be typed as Map<String,String> (rather than <String,Any>
   // as the Hocon persistence library we're using can't deserialize values from disk into
   // an Any.  If this causes issues, we'll need to wrap the deserialization to coerce numbers from strings.
   val connectionParameters: Map<ConnectionParameterName, String>
) : JdbcConnectionConfiguration {
   companion object {
      fun forParams(
         connectionName: String,
         driver: DatabaseDriverName,
         connectionParameters: Map<IConnectionParameter, String>
      ): DefaultJdbcConnectionConfiguration {
         return DefaultJdbcConnectionConfiguration(
            connectionName,
            driver,
            connectionParameters.mapKeys { it.key.templateParamName })
      }
   }

   override fun getConnectionParameter(parameter: IConnectionParameter): String {
      return connectionParameters[parameter.param.templateParamName] ?: error("No parameter ${parameter.param.templateParamName} defined")
   }

   override fun getConnectionParameterOrNull(parameter: IConnectionParameter): String? {
      return connectionParameters[parameter.param.templateParamName]
   }

   override fun connectionPoolProperties(): ConnectionPoolProperties {
      return JdbcConnectionPoolParameters.connectionPoolProperties(connectionParameters)
   }

   override fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder): JdbcUrlAndCredentials {
      return urlBuilder.build(connectionParameters)
   }

   override fun getUiDisplayProperties(): Map<String, Any> {
      return connectionParameters.obfuscateKeys("password")
   }
}

@JsonDeserialize(`as` = DefaultJdbcConnectionConfiguration::class)
interface JdbcConnectionConfiguration : ConnectorConfiguration {
   override val connectionName: String

   /**
    * Exists for backwards compatibility of everyone's connections.conf files
    */
   val jdbcDriver: DatabaseDriverName
   fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder): JdbcUrlAndCredentials

   override val driverName: DatabaseDriverName
      get() = jdbcDriver
   override val type: ConnectorCategory
      get() = ConnectorCategory.JDBC

   fun getConnectionParameter(parameter: IConnectionParameter):String
   fun getConnectionParameterOrNull(parameter: IConnectionParameter):String?

   fun connectionPoolProperties(): ConnectionPoolProperties
}


