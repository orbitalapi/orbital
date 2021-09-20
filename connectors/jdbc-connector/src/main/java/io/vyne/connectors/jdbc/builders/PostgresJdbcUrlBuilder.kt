package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.jdbc.IJdbcConnectionParamEnum
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.JdbcConnectionParameterName
import io.vyne.connectors.jdbc.JdbcUrlBuilder
import io.vyne.connectors.jdbc.SimpleDataType
import io.vyne.connectors.jdbc.connectionParams

class PostgresJdbcUrlBuilder : JdbcUrlBuilder {
   enum class Parameters(override val param: JdbcConnectionParam) : IJdbcConnectionParamEnum {
      HOST(JdbcConnectionParam("host", SimpleDataType.STRING)),
      PORT(JdbcConnectionParam("port", SimpleDataType.NUMBER, defaultValue = 5432)),
      DATABASE(JdbcConnectionParam("database", SimpleDataType.STRING)),
      USERNAME(JdbcConnectionParam("username", SimpleDataType.STRING, required = false)),
      PASSWORD(JdbcConnectionParam("password", SimpleDataType.STRING, required = false, sensitive = true))
   }

   override val displayName: String = "Postgres"
   override val driverName: String = "org.postgresql.Driver"
   override val parameters: List<JdbcConnectionParam> = Parameters.values().connectionParams()

   override fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = JdbcUrlBuilder.assertAllParametersPresent(parameters, inputs)

//      jdbc:postgresql://localhost:5432/vynedb
      val connectionString = "jdbc:postgresql://{host}:{port}/{database}".substitute(inputsWithDefaults)
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

fun String.substitute(inputs: Map<String, Any>): String {
   return inputs.entries.fold(this) { acc, entry ->
      val (key, value) = entry
      acc.replace("{$key}", value.toString())
   }
}
