package com.orbitalhq.connectors.jdbc.drivers.mysql

import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.jdbc.drivers.postgres.PostgresJdbcUrlBuilder.Parameters
import com.orbitalhq.connectors.jdbc.drivers.postgres.remove
import com.orbitalhq.utils.substitute

object MySqlJdbcUrlBuilder : JdbcUrlBuilder{
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      HOST(ConnectionDriverParam("host", SimpleDataType.STRING)),
      PORT(ConnectionDriverParam("port", SimpleDataType.NUMBER, defaultValue = 3306)),
      DATABASE(ConnectionDriverParam("database", SimpleDataType.STRING)),
      USERNAME(ConnectionDriverParam("username", SimpleDataType.STRING, required = false)),
      PASSWORD(ConnectionDriverParam("password", SimpleDataType.STRING, required = false, sensitive = true))
   }

   override val displayName: String = "MySQL"
   override val driverName: String = "com.mysql.cj.jdbc"
   override val parameters: List<ConnectionDriverParam> =  Parameters.values().connectionParams()

   override fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = ConnectorUtils.assertAllParametersPresent(parameters, inputs)

      val connectionString = "jdbc:mysql://{host}:{port}/{database}".substitute(inputsWithDefaults)
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
