package io.vyne.connectors.jdbc

import com.google.common.cache.CacheBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.JDBCUtils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource


interface JdbcConnectionFactory {
   fun dataSource(connectionName: String): DataSource
   fun jdbcTemplate(connectionName: String): NamedParameterJdbcTemplate

   fun dataSource(connectionConfiguration: JdbcConnectionConfiguration): DataSource
   fun jdbcTemplate(connectionConfiguration: JdbcConnectionConfiguration): NamedParameterJdbcTemplate

   fun config(connectionName: String): JdbcConnectionConfiguration

   fun dsl(connectionConfiguration: JdbcConnectionConfiguration): DSLContext {
      val dialect = JDBCUtils.dialect(connectionConfiguration.buildUrlAndCredentials().url)
      val datasource = dataSource(connectionConfiguration)
      return DSL.using(datasource, dialect)
   }
}

/**
 * A Vyne JdbcConnectionFactory which wraps access into a Hikari connection pool.
 * Prefer using this implementation in prod code whenever doing anything like querying / inserting
 */
class HikariJdbcConnectionFactory(
   private val connectionRegistry: JdbcConnectionRegistry,
   private val hikariConfigTemplate: HikariConfig,
   private val metricsFactory: MicrometerMetricsTrackerFactory? = null
) : JdbcConnectionFactory {
   private val dataSourceCache = CacheBuilder.newBuilder()
      .build<String, DataSource>()

   override fun config(connectionName: String): JdbcConnectionConfiguration =
      connectionRegistry.getConnection(connectionName)

   override fun dataSource(connectionName: String): DataSource {
      return dataSourceCache.get(connectionName) {
         val connection = connectionRegistry.getConnection(connectionName)
         val url = connection.buildUrlAndCredentials()
         val hikariConfig = HikariConfig(hikariConfigTemplate.dataSourceProperties)
         hikariConfig.poolName = "HikariPool-$connectionName"
         hikariConfig.jdbcUrl = url.url
         hikariConfig.username = url.username
         hikariConfig.password = url.password
         hikariConfig.metricsTrackerFactory = metricsFactory
         HikariDataSource(hikariConfig)
      }
   }

   override fun dataSource(connectionConfiguration: JdbcConnectionConfiguration): DataSource {
      return dataSource(connectionConfiguration.connectionName)
   }

   override fun jdbcTemplate(connectionName: String): NamedParameterJdbcTemplate {
      val dataSource = dataSource(connectionName)
      return NamedParameterJdbcTemplate(dataSource)
   }

   override fun jdbcTemplate(connectionConfiguration: JdbcConnectionConfiguration): NamedParameterJdbcTemplate {
      return NamedParameterJdbcTemplate(dataSource(connectionConfiguration.connectionName))
   }

}

/**
 * Simple connection factory.  Does not support pooling, so is not advisable for
 * any transactional or query work.
 */
class SimpleJdbcConnectionFactory() : JdbcConnectionFactory {
   override fun dataSource(connectionName: String): DataSource {
      error("Not supported on DefaultJdbcConnectionFactory")
   }


   override fun dataSource(connectionConfiguration: JdbcConnectionConfiguration): DataSource {
      val connectionParams = connectionConfiguration.buildUrlAndCredentials()
      return DriverManagerDataSource(connectionParams.url, connectionParams.username, connectionParams.password)
   }

   override fun jdbcTemplate(connectionName: String): NamedParameterJdbcTemplate {
      error("Not supported on DefaultJdbcConnectionFactory")
   }

   override fun jdbcTemplate(connectionConfiguration: JdbcConnectionConfiguration): NamedParameterJdbcTemplate {
      return NamedParameterJdbcTemplate(dataSource(connectionConfiguration))
   }

   override fun config(connectionName: String): JdbcConnectionConfiguration {
      TODO("Not yet implemented")
   }
}





