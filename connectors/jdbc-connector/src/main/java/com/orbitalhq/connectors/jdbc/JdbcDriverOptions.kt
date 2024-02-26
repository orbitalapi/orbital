package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.jdbc.drivers.JdbcUrlBuilders
import com.orbitalhq.connectors.registry.ConnectorType

object JdbcDriverOptions {
   val driverOptions : List<ConnectionDriverOptions> = JdbcDriver.values().map { driver ->
      val builder = JdbcUrlBuilders.forDriver(driver)
      ConnectionDriverOptions(driver.name, builder.displayName, ConnectorType.JDBC, builder.parameters)
   }
}
