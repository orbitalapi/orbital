package com.orbitalhq.connectors.jdbc.drivers

import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlBuilder

object JdbcUrlBuilders {
   @Deprecated("Use DatabaseSupport.forDriver().jdbcUrlBuilder()")
   fun forDriver(driver: JdbcDriver): JdbcUrlBuilder {
      // Making this API backwards compatible.
      // We can go direct to DatabaseSupport now.
      return DatabaseSupport.forDriver(driver).jdbcUrlBuilder()
   }
}
