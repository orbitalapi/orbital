package com.orbitalhq.connectors.jdbc.drivers.mssql

import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.IConnectionParameter
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.connectionParams
import com.orbitalhq.connectors.jdbc.drivers.postgres.remove
import com.orbitalhq.utils.substitute

class MssqlJdbcUrlBuilder : JdbcUrlBuilder {
   //  "jdbc:sqlserver://localhost:14330;database=Northwind;encrypt=true;trustServerCertificate=true;",
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      HOST(ConnectionDriverParam("host", SimpleDataType.STRING)),
      PORT(ConnectionDriverParam("port", SimpleDataType.NUMBER, defaultValue = 1433)),
      SCHEMA(ConnectionDriverParam("schema", SimpleDataType.STRING, defaultValue = "dbo", required = false)),
      DATABASE(ConnectionDriverParam("database", SimpleDataType.STRING)),
      USERNAME(ConnectionDriverParam("username", SimpleDataType.STRING, required = false)),
      PASSWORD(ConnectionDriverParam("password", SimpleDataType.STRING, required = false, sensitive = true)),
      ENCRYPT(ConnectionDriverParam("Encrypt traffic", SimpleDataType.BOOLEAN, defaultValue = true, templateParamName = "encrypt")),
      TRUST_CERTIFICATE(ConnectionDriverParam("Trust SQL Server certificate", SimpleDataType.BOOLEAN, defaultValue = true, templateParamName = "trustServerCertificate")),
   }
   override val displayName: String = "Sql Server"
   override val driverName: String = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
   override val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()

   override fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = ConnectorUtils.assertAllParametersPresent(parameters, inputs)

      val connectionString = "jdbc:sqlserver://{host}:{port}".substitute(inputsWithDefaults)
      val remainingInputs = inputsWithDefaults.remove(listOf("host", "port", "username", "password","schema"))
         .entries.joinToString(separator = ";") { (key, value) -> "$key=$value" }
      val builtConnectionString = if (remainingInputs.isNullOrEmpty()) {
         connectionString
      } else {
         "$connectionString;$remainingInputs"
      }

      return JdbcUrlAndCredentials(
         builtConnectionString,
         inputsWithDefaults["username"]?.toString(),
         inputsWithDefaults["password"]?.toString()
      )
   }
}
