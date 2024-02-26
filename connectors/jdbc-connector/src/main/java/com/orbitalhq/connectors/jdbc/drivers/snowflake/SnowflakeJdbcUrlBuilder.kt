package com.orbitalhq.connectors.jdbc.drivers.snowflake

import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.jdbc.drivers.postgres.remove
import com.orbitalhq.utils.substitute

class SnowflakeJdbcUrlBuilder : JdbcUrlBuilder {
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      ACCOUNT(ConnectionDriverParam("account", SimpleDataType.STRING)),
      DATABASE(ConnectionDriverParam("db", SimpleDataType.STRING)),
      SCHEMA_NAME(ConnectionDriverParam("schema", SimpleDataType.STRING)),
      WAREHOUSE_NAME(ConnectionDriverParam("warehouse", SimpleDataType.STRING)),
      USERNAME(ConnectionDriverParam("username", SimpleDataType.STRING)),
      PASSWORD(ConnectionDriverParam("password", SimpleDataType.STRING, sensitive = true)),
      ROLE(ConnectionDriverParam("role", SimpleDataType.STRING))
   }

   override val displayName: String = "Snowflake"
   override val driverName: String = "net.snowflake.client.jdbc.SnowflakeDriver"
   override val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()

   override fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = ConnectorUtils.assertAllParametersPresent(parameters, inputs)

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
