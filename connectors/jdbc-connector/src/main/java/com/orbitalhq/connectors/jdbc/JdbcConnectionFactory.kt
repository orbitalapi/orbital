package com.orbitalhq.connectors.jdbc

import com.google.common.cache.CacheBuilder
import com.orbitalhq.config.UpdatableConfigRepository
import com.orbitalhq.connectors.config.jdbc.ConnectionPoolProperties
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.drivers.DatabaseSupport
import com.orbitalhq.connectors.jdbc.registry.JdbcConnectionRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.conf.RenderQuotedNames
import org.jooq.conf.Settings
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
      DatabaseSupport.forDriverName(connectionConfiguration.jdbcDriver)
      val dialect = JDBCUtils.dialect(connectionConfiguration.buildUrlAndCredentials().url)
      val datasource = dataSource(connectionConfiguration)
      return DSL.using(
         datasource, dialect, Settings()
            .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
      )
   }
}

private val logger = KotlinLogging.logger { }

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
      .build<String, HikariDataSource>()

   init {
      logger.info { "New HikariJdbcConnectionFactory created" }
      if (connectionRegistry is UpdatableConfigRepository<*>) {
         connectionRegistry.configUpdated.subscribe {
            logger.info { "Connection registry changed, invalidating data source cache" }
            val jdbcConnectionConfiguration = it as JdbcConnectionConfiguration
            val cacheKey = jdbcConnectionConfiguration.connectionName
            dataSourceCache.getIfPresent(cacheKey)?.let { dataSource ->
               val (hikariConfig, connectionPoolProps) = this.hikariConfigFor(cacheKey)
               if (hikariConfig.jdbcUrl != dataSource.jdbcUrl ||
                   connectionPoolProps.connectionTimeout != dataSource.connectionTimeout ||
                   connectionPoolProps.minimumIdleConnections != dataSource.minimumIdle ||
                   connectionPoolProps.maxLifeTime != dataSource.maxLifetime ||
                   connectionPoolProps.maxPoolSize != dataSource.maximumPoolSize ||
                   connectionPoolProps.idleTimeout != dataSource.idleTimeout
                  ) {
                  logger.info { "DataSource properties for $cacheKey updated, removing the existing datasource from cache!" }
                  dataSourceCache.invalidate(cacheKey)
                  dataSource.close()
                  cacheKey
               } else {
                  logger.info { "DataSource properties for $cacheKey NOT updated, keeping the existing datasource in cache!" }
               }
               cacheKey
            }?.let {
               logger.info { "There is no datasource for $it in the cache, no update is required." }
            }
         }
      }
   }

   override fun config(connectionName: String): JdbcConnectionConfiguration =
      connectionRegistry.getConnection(connectionName)

   override fun dataSource(connectionName: String): DataSource {
      return dataSourceCache.get(connectionName) {
         logger.info { "Creating HikariDataSource for $connectionName" }
         val (hikariConfig, connectionPoolProps) = this.hikariConfigFor(connectionName)
         logger.info { "Updated connection pool properties for $connectionName with $connectionPoolProps" }
         HikariDataSource(hikariConfig)
      }
   }

   private fun hikariConfigFor(connectionName: String): Pair<HikariDataSource, ConnectionPoolProperties> {
      val connection = connectionRegistry.getConnection(connectionName)
      val url = connection.buildUrlAndCredentials()
      val hikariConfig = HikariConfig(hikariConfigTemplate.dataSourceProperties)
      hikariConfig.poolName = "HikariPool-$connectionName"
      hikariConfig.jdbcUrl = url.url
      hikariConfig.username = url.username
      hikariConfig.password = url.password
      hikariConfig.metricsTrackerFactory = metricsFactory
      val connectionPoolProps = connection.decorateHikariConnectionPool(hikariConfig)
      return HikariDataSource(hikariConfig) to connectionPoolProps
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
      return DriverManagerDataSource(connectionParams.url, connectionParams.username!!, connectionParams.password!!)
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





