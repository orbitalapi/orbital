package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.jdbc.IJdbcConnectionParamEnum
import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.JdbcConnectionParameterName
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcUrlBuilder
import io.vyne.connectors.jdbc.SimpleDataType
import io.vyne.connectors.jdbc.connectionParams

class SnowflakeJdbcUrlBuilder : JdbcUrlBuilder {
   enum class Parameters(override val param: JdbcConnectionParam) : IJdbcConnectionParamEnum {
      ACCOUNT(JdbcConnectionParam("account", SimpleDataType.STRING)),
      DATABASE(JdbcConnectionParam("db", SimpleDataType.STRING)),
      SCHEMA_NAME(JdbcConnectionParam("schema", SimpleDataType.STRING)),
      WAREHOUSE_NAME(JdbcConnectionParam("warehouse", SimpleDataType.STRING)),
      USERNAME(JdbcConnectionParam("username", SimpleDataType.STRING)),
      PASSWORD(JdbcConnectionParam("password", SimpleDataType.STRING, sensitive = true)),
      ROLE(JdbcConnectionParam("role", SimpleDataType.STRING))
   }

   override val displayName: String = "Snowflake"
   override val driverName: String = "net.snowflake.client.jdbc.SnowflakeDriver"
   override val parameters: List<JdbcConnectionParam> = Parameters.values().connectionParams()

   override fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = JdbcUrlBuilder.assertAllParametersPresent(parameters, inputs)

      val connectionString = "jdbc:snowflake://{account}.snowflakecomputing.com/".substitute(inputsWithDefaults)
      val remainingInputs = inputsWithDefaults.remove(listOf("account", "host", "username", "password", "user"))
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
