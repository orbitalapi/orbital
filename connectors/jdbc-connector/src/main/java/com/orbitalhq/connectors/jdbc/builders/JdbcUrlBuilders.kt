package com.orbitalhq.connectors.jdbc.builders

import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder
import com.orbitalhq.connectors.config.jdbc.JdbcDriver

object JdbcUrlBuilders {
   fun forDriver(driver: JdbcDriver): JdbcUrlBuilder {
      return when (driver) {
         JdbcDriver.H2 -> H2JdbcUrlBuilder()
         JdbcDriver.POSTGRES -> PostgresJdbcUrlBuilder()
         JdbcDriver.REDSHIFT -> RedshiftJdbcUrlBuilder()
         JdbcDriver.SNOWFLAKE -> SnowflakeJdbcUrlBuilder()
      }
   }
}
