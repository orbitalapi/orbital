package com.orbitalhq.connectors.config.jdbc

import com.orbitalhq.connectors.IConnectionParameter

/**
 * Another test class, useful where we want to shortcut the config, because the
 * JDBC Url has already been explitily provided to us
 */
data class JdbcUrlCredentialsConnectionConfiguration(
   override val connectionName: String,
   override val jdbcDriver: DatabaseDriverName,
   val urlAndCredentials: JdbcUrlAndCredentials,
   val params: Map<IConnectionParameter, String> = emptyMap()
) : JdbcConnectionConfiguration {
   override fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder) = urlAndCredentials
   override fun getConnectionParameter(parameter: IConnectionParameter): String {
      return params[parameter]!!
   }

   override fun getUiDisplayProperties(): Map<String, Any> {
      TODO("Not yet implemented")
   }

   override fun getConnectionParameterOrNull(parameter: IConnectionParameter): String? {
      return params[parameter]
   }
}
