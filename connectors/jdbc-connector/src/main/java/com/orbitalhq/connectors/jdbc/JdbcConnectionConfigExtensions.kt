package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport

// Has to be an extension function, because the interface lives in the parent package
val JdbcConnectionConfiguration.address: String
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
