package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.jdbc.JdbcConnectionBuilder
import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.JdbcConnectionParameterName
import io.vyne.connectors.jdbc.JdbcConnectionString
import lang.taxi.types.PrimitiveType

class PostgresJdbcConnectionBuilder : JdbcConnectionBuilder {
   override val displayName: String = "Postgres"
   override val driverName: String = "org.postgresql.Driver"
   override val parameters: List<JdbcConnectionParam> = listOf(
      JdbcConnectionParam("host", PrimitiveType.STRING),
      JdbcConnectionParam("port", PrimitiveType.INTEGER, defaultValue = 5432),
      JdbcConnectionParam("database", PrimitiveType.STRING),
      JdbcConnectionParam("username", PrimitiveType.STRING, required = false),
      JdbcConnectionParam("password", PrimitiveType.STRING, required = false, sensitive = true),
   )

   override fun build(inputs: Map<JdbcConnectionParameterName, Any>): JdbcConnectionString {
      val inputsWithDefaults = JdbcConnectionBuilder.assertAllParametersPresent(parameters, inputs)

//      jdbc:postgresql://localhost:5432/vynedb
      val connectionString = "jdbc:postgresql://{host}:{port}/{database}".substitute(inputsWithDefaults)
      val remainingInputs = inputsWithDefaults.remove(listOf("host", "port", "database"))
         .entries.joinToString(separator = "&") { (key, value) -> "$key=$value" }
      return if (remainingInputs.isNullOrEmpty()) {
         connectionString
      } else {
         "$connectionString?$remainingInputs"
      }
   }
}

private fun <K, V> Map<K, V>.remove(keysToExclude: List<K>): Map<K, V> {
   return filterKeys { !keysToExclude.contains(it) };
}

private fun String.substitute(inputs: Map<String, Any>): String {
   return inputs.entries.fold(this) { acc, entry ->
      val (key, value) = entry
      acc.replace("{$key}", value.toString())
   }
}
