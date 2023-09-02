package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.builders.JdbcUrlBuilders

// Has to be an extension function, because the interface lives in the parent package
val JdbcConnectionConfiguration.address: String
   get() {
      return this.buildUrlAndCredentials(this.urlBuilder).url
   }

val JdbcConnectionConfiguration.urlBuilder: JdbcUrlBuilder
   get() {
      return JdbcUrlBuilders.forDriver(this.jdbcDriver)
   }

fun JdbcConnectionConfiguration.buildUrlAndCredentials(): JdbcUrlAndCredentials {
   return this.buildUrlAndCredentials(this.urlBuilder)
}
