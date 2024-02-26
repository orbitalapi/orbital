package com.orbitalhq.connectors.jdbc.drivers.snowflake

import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport

object SnowflakeDatabaseSupport : DatabaseSupport {
   override fun jdbcUrlBuilder(): JdbcUrlBuilder = SnowflakeJdbcUrlBuilder()
}
