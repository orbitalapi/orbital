package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.jdbc.JdbcConnectionDetails
import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.JdbcConnectionParameterName
import io.vyne.connectors.jdbc.JdbcUrlBuilder
import io.vyne.connectors.jdbc.SimpleDataType

class PostgresJdbcUrlBuilder : JdbcUrlBuilder {
   override val displayName: String = "Postgres"
   override val driverName: String = "org.postgresql.Driver"
   override val parameters: List<JdbcConnectionParam> = listOf(
      JdbcConnectionParam("host", SimpleDataType.STRING),
      JdbcConnectionParam("port", SimpleDataType.NUMBER, defaultValue = 5432),
      JdbcConnectionParam("database", SimpleDataType.STRING),
      JdbcConnectionParam("username", SimpleDataType.STRING, required = false),
      JdbcConnectionParam("password", SimpleDataType.STRING, required = false, sensitive = true),
   )

   override fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcConnectionDetails {
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
      return JdbcConnectionDetails(
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
