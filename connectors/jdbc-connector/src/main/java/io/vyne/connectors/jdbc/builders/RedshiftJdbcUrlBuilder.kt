package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.connectionParams
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcUrlBuilder

class RedshiftJdbcUrlBuilder : JdbcUrlBuilder {
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      HOST(ConnectionDriverParam("host", SimpleDataType.STRING)),
      PORT(ConnectionDriverParam("port", SimpleDataType.NUMBER, defaultValue = 5439)),
      DATABASE(ConnectionDriverParam("database", SimpleDataType.STRING)),
      USERNAME(ConnectionDriverParam("username", SimpleDataType.STRING, required = false)),
      PASSWORD(ConnectionDriverParam("password", SimpleDataType.STRING, required = false, sensitive = true))
   }

   override val displayName: String = "Redshift"
   override val driverName: String = "com.amazon.redshift.jdbc42.Driver"
   override val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()

   override fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = JdbcUrlBuilder.assertAllParametersPresent(parameters, inputs)

      val connectionString = "jdbc:redshift://{host}:{port}/{database}".substitute(inputsWithDefaults)
      val remainingInputs = inputsWithDefaults.remove(listOf("host", "port", "database", "username", "password"))
         .entries.joinToString(separator = "&") { (key, value) -> "$key=$value" }
      val builtConnectionString = if (remainingInputs.isNullOrEmpty()) {
         connectionString
      } else {
         "$connectionString?$remainingInputs"
      }

      return JdbcUrlAndCredentials(
         builtConnectionString,
         inputsWithDefaults["username"]?.toString(),
         inputsWithDefaults["password"]?.toString()
      )
   }
}

private fun <K, V> Map<K, V>.remove(keysToExclude: List<K>): Map<K, V> {
   return filterKeys { !keysToExclude.contains(it) };
}
