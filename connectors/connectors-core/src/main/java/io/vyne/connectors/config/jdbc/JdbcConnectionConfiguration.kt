package io.vyne.connectors.config.jdbc

import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import kotlinx.serialization.Serializable


/**
 * Represents a persistable jdbc connection with parameters.
 * This should be used to create an actual connection to the db
 */
@Serializable
data class DefaultJdbcConnectionConfiguration(
   override val connectionName: String,
   override val jdbcDriver: JdbcDriver,
   // connectionParameters must be typed as Map<String,String> (rather than <String,Any>
   // as the Hocon persistence library we're using can't deserialize values from disk into
   // an Any.  If this causes issues, we'll need to wrap the deserialization to coerce numbers from strings.
   val connectionParameters: Map<ConnectionParameterName, String>
) : JdbcConnectionConfiguration {
   companion object {
      fun forParams(
         connectionName: String,
         driver: JdbcDriver,
         connectionParameters: Map<IConnectionParameter, String>
      ): DefaultJdbcConnectionConfiguration {
         return DefaultJdbcConnectionConfiguration(
            connectionName,
            driver,
            connectionParameters.mapKeys { it.key.templateParamName })
      }
   }

   override fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder): JdbcUrlAndCredentials {
      return urlBuilder.build(connectionParameters)
   }
}

interface JdbcConnectionConfiguration : ConnectorConfiguration {
   override val connectionName: String
   val jdbcDriver: JdbcDriver
   fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder): JdbcUrlAndCredentials

   override val driverName: String
      get() = jdbcDriver.name
   override val type: ConnectorType
      get() = ConnectorType.JDBC
}


