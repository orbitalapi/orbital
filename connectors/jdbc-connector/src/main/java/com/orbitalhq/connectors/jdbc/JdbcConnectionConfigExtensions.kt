package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.config.jdbc.ConnectionPoolProperties
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.zaxxer.hikari.HikariConfig

// Has to be an extension function, because the interface lives in the parent package
val JdbcConnectionConfiguration.address:  String
   get() {
      return this.buildUrlAndCredentials(this.urlBuilder).url
   }

val JdbcConnectionConfiguration.urlBuilder: JdbcUrlBuilder
   get() {
      return DatabaseSupport.forDriverName(this.driverName).jdbcUrlBuilder()
   }

fun JdbcConnectionConfiguration.buildUrlAndCredentials(): JdbcUrlAndCredentials {
   return this.buildUrlAndCredentials(this.urlBuilder)
}

fun JdbcConnectionConfiguration.decorateHikariConnectionPool(config: HikariConfig): ConnectionPoolProperties {
   val connectionPoolProperties = this.connectionPoolProperties()
   config.maximumPoolSize = connectionPoolProperties.maxPoolSize
   config.idleTimeout = connectionPoolProperties.idleTimeout
   config.minimumIdle = connectionPoolProperties.minimumIdleConnections
   config.connectionTimeout = connectionPoolProperties.connectionTimeout
   config.maxLifetime = connectionPoolProperties.maxLifeTime
   return connectionPoolProperties
}
