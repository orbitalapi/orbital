package com.orbitalhq.connectors.jdbc.drivers.snowflake

import com.orbitalhq.connectors.config.jdbc.JdbcMetadataParams
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.config.jdbc.DatabaseDriverName
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport

object SnowflakeDatabaseSupport : DatabaseSupport {
   override val jdbcDriverMetadata: JdbcMetadataParams = JdbcMetadataParams()
   override val driverName: DatabaseDriverName = "SNOWFLAKE"
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = SnowflakeJdbcUrlBuilder()
}
