package com.orbitalhq.cockpit.core.db.migrations

import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.postgresql.Driver
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.SimpleDriverDataSource

/**
 * Seems because we're using r2dbc, we have to manually wire Flyway, as
 * Spring Boot doesn't handle it for us.
 *
 * Left over from that time we tried to use r2dbc, and found ANOTHER things that didn't work
 * was Flyway. :(  Keeping it here to make the next trip around the merry-go-round faster
 */
//@Configuration
//@EnableConfigurationProperties(R2dbcProperties::class)
//class FlywayConfig {
//
//   @Bean
//   fun flywayDatasource(r2dbcProperties: R2dbcProperties):DataSource {
//      val jdbcUrl = r2dbcProperties.url.replace("r2dbc:", "jdbc:")
//      return SimpleDriverDataSource(
//         Driver(),
//         jdbcUrl,
//         r2dbcProperties.username,
//         r2dbcProperties.password
//      )
//   }
//}
