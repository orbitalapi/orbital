package io.vyne.connectors.config.jdbc

import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.ConnectionParameterName

/**
 * Builds a Jdbc connection string substituting parameters
 */
interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<ConnectionDriverParam>

   fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials
}
