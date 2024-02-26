package com.orbitalhq.connectors.jdbc.drivers.redshift

import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport

object RedshiftDatabaseSupport : DatabaseSupport {
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = RedshiftJdbcUrlBuilder()
}
