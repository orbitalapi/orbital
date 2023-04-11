package io.vyne.connectors.jdbc

import io.vyne.connectors.ConnectionDriverOptions
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.connectors.config.jdbc.JdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.JdbcUrlBuilders
import io.vyne.connectors.registry.ConnectorType

object JdbcDriverOptions {
   val driverOptions : List<ConnectionDriverOptions> = JdbcDriver.values().map { driver ->
      val builder = JdbcUrlBuilders.forDriver(driver)
      ConnectionDriverOptions(driver.name, builder.displayName, ConnectorType.JDBC, builder.parameters)
   }
}
