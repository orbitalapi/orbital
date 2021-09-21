package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.JdbcConnectionParameterName
import io.vyne.connectors.jdbc.JdbcUrlBuilder
import io.vyne.connectors.jdbc.SimpleDataType

class H2JdbcUrlBuilder : JdbcUrlBuilder {
   override val displayName: String = "H2"
   override val driverName: String = "org.h2.Driver"
   override val parameters: List<JdbcConnectionParam> = listOf(
      JdbcConnectionParam("catalog", SimpleDataType.STRING),
      JdbcConnectionParam("username", SimpleDataType.STRING, "sa"),
      JdbcConnectionParam("password", SimpleDataType.STRING, ""),
   )

   override fun build(inputs: Map<JdbcConnectionParameterName, Any?>): JdbcUrlAndCredentials {
      val inputsWithDefaults = JdbcUrlBuilder.assertAllParametersPresent(parameters, inputs)
      val connectionString = "jdbc:h2:mem:{catalog}".substitute(inputsWithDefaults)
      return JdbcUrlAndCredentials(
         connectionString,
         username = inputsWithDefaults["username"]?.toString(),
         password = inputsWithDefaults["password"]?.toString()
      )
   }
}
