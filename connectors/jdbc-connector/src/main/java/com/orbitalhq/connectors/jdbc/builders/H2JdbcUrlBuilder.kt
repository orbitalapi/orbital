package com.orbitalhq.connectors.jdbc.builders

import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName
import com.orbitalhq.connectors.ConnectorUtils
import com.orbitalhq.connectors.SimpleDataType
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.utils.substitute

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
