package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.DatabaseMetaData

/**
 * Super simple config for a pre-wired Jdbc URL.
 * Does not support parameter building in the UI.
 * Useful for tests, where the NamedTemplate has already been constructured for us
 */
data class NamedTemplateConnection(
   override val connectionName: String,
   val template: NamedParameterJdbcTemplate,
   override val jdbcDriver: JdbcDriver = JdbcDriver.H2
) : JdbcConnectionConfiguration {
   private val metadata: DatabaseMetaData by lazy {

      // Be careful with the connection - if we dont close it, we'll exhaust the connection pool
      template.jdbcTemplate.dataSource!!.connection.use {
         it.metaData
      }
   }

   override fun buildUrlAndCredentials(urlBuilder: JdbcUrlBuilder): JdbcUrlAndCredentials {
      return JdbcUrlAndCredentials(
          metadata.url,
          metadata.userName,
          ""
      )
   }

}
