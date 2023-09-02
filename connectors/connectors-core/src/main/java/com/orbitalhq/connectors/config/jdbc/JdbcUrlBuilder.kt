package com.orbitalhq.connectors.config.jdbc

import com.orbitalhq.connectors.ConnectionDriverParam
import com.orbitalhq.connectors.ConnectionParameterName

/**
 * Builds a Jdbc connection string substituting parameters
 */
interface JdbcUrlBuilder {
   val displayName: String
   val driverName: String
   val parameters: List<ConnectionDriverParam>

   fun build(inputs: Map<ConnectionParameterName, Any?>): JdbcUrlAndCredentials
}
