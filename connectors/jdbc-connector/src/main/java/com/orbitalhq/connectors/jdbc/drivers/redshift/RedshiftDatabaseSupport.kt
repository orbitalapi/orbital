package com.orbitalhq.connectors.jdbc.drivers.redshift

import com.orbitalhq.connectors.config.jdbc.JdbcMetadataParams
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.config.jdbc.DatabaseDriverName
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport

object RedshiftDatabaseSupport : DatabaseSupport {
   override val driverName: DatabaseDriverName = "REDSHIFT"
   override val jdbcDriverMetadata: JdbcMetadataParams = JdbcMetadataParams()
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = RedshiftJdbcUrlBuilder()
}
