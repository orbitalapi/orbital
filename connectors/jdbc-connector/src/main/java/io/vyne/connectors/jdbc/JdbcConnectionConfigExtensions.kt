package io.vyne.connectors.jdbc

import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.config.jdbc.JdbcUrlBuilder
import io.vyne.connectors.jdbc.builders.JdbcUrlBuilders

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
