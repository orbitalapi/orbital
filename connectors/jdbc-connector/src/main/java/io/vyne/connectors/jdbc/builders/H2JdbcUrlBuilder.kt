package io.vyne.connectors.jdbc.builders

import io.vyne.connectors.jdbc.JdbcConnectionParam
import io.vyne.connectors.jdbc.JdbcConnectionParameterName
import io.vyne.connectors.jdbc.JdbcUrlBuilder

class H2JdbcUrlBuilder : JdbcUrlBuilder {
   override val displayName: String = "H2"
   override val driverName: String = "org.h2.Driver"
   override val parameters: List<JdbcConnectionParam> = listOf(

   )

   override fun build(inputs: Map<JdbcConnectionParameterName, Any?>): String {
      TODO("Not yet implemented")
   }
}
