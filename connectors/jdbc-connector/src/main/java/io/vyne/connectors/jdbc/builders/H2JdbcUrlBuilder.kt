package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName
import io.vyne.connectors.ConnectorUtils
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcUrlBuilder

class H2JdbcUrlBuilder : JdbcUrlBuilder {
   override val displayName: String = "H2"
   override val driverName: String = "org.h2.Driver"
   override val parameters: List<ConnectionDriverParam> = listOf(
      ConnectionDriverParam("catalog", SimpleDataType.STRING),
      ConnectionDriverParam("username", SimpleDataType.STRING, "sa"),
      ConnectionDriverParam("password", SimpleDataType.STRING, ""),
   )

   override fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = ConnectorUtils.assertAllParametersPresent(parameters, inputs)
      val connectionString = "jdbc:h2:mem:{catalog}".substitute(inputsWithDefaults)
      return JdbcUrlAndCredentials(
         connectionString,
         username = inputsWithDefaults["username"]?.toString(),
         password = inputsWithDefaults["password"]?.toString()
      )
   }
}
