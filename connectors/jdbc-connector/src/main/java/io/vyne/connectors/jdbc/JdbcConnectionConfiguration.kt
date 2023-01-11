package io.vyne.connectors.jdbc

import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.registry.ConnectorConfiguration
import io.vyne.connectors.registry.ConnectorType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.DatabaseMetaData


/**
 * Represents a persistable jdbc connection with parameters.
 * This should be used to create an actual connection to the db
 */
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

   override val address: String = buildUrlAndCredentials().url

   override fun buildUrlAndCredentials(): JdbcUrlAndCredentials {
      return jdbcDriver.urlBuilder().build(connectionParameters)
   }
}

interface JdbcConnectionConfiguration : ConnectorConfiguration {
   override val connectionName: String
   val jdbcDriver: JdbcDriver
   fun buildUrlAndCredentials(): JdbcUrlAndCredentials

   val address: String
   override val driverName: String
      get() = jdbcDriver.name
   override val type: ConnectorType
      get() = ConnectorType.JDBC
}

data class JdbcUrlAndCredentials(
   val url: String,
   val username: String?,
   val password: String?
)

/**
 * Another test class, useful where we want to shortcut the config, because the
 * JDBC Url has already been explitily provided to us
 */
data class JdbcUrlCredentialsConnectionConfiguration(
   override val connectionName: String,
   override val jdbcDriver: JdbcDriver,
   val urlAndCredentials: JdbcUrlAndCredentials
) : JdbcConnectionConfiguration {
   override fun buildUrlAndCredentials() = urlAndCredentials

   override val address: String = urlAndCredentials.url

}

/**
 * Super simple config for a pre-wired Jdbc URL.
 * Does not support parameter building in the UI.
 * Useful for tests, where the NamedTemplate has already been constructured for us
 */
data class NamedTemplateConnection(
   override val connectionName: String,
   val template: NamedParameterJdbcTemplate,
   override val jdbcDriver: JdbcDriver = JdbcDriver.H2
) : JdbcConnectionConfiguration {
   private val metadata: DatabaseMetaData by lazy {

      // Be careful with the connection - if we dont close it, we'll exhaust the connection pool
      template.jdbcTemplate.dataSource!!.connection.use {
         it.metaData
      }
   }

   override fun buildUrlAndCredentials(): JdbcUrlAndCredentials {
      return JdbcUrlAndCredentials(
         metadata.url,
         metadata.userName,
         ""
      )
   }

   override val address: String = buildUrlAndCredentials().url
}
