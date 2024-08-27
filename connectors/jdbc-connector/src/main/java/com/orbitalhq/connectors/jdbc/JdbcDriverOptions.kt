package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.registry.ConnectorType

object JdbcDriverOptions {
   val driverOptions : List<ConnectionDriverOptions>
      get() = DatabaseSupport.drivers.map { driver ->
         val builder = driver.jdbcUrlBuilder()
         ConnectionDriverOptions(driver.driverName, builder.displayName, ConnectorType.JDBC, builder.parameters)
      }
}
