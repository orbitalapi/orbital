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
         connectionRegistry.configUpdated.subscribe { currentState ->
            logger.info { "Connection registry changed, invalidating data source cache" }
            // This code was recently changed expecting a JdbcConnectionConfiguration,
            // but can't see why ...
            val keysToInvalidate = when (currentState) {
               is Map<*, *> -> currentState.keys as Collection<String>
               is JdbcConnectionConfiguration -> listOf(currentState.connectionName)
               else -> {
                  logger.error { "Unexpected type of update message - got ${currentState::class.simpleName} - cannot invalidate caches" }
                  emptyList()
               }
            }
            keysToInvalidate.forEach { connectionName ->
               dataSourceCache.getIfPresent(connectionName)?.let { dataSource ->
                  if (configChanged(connectionName, dataSource)) {
                     logger.info { "DataSource properties for $connectionName updated, removing the existing datasource from cache" }
                     dataSourceCache.invalidate(connectionName)
                     dataSource.close()
                     connectionName
                  } else {
                     logger.debug { "DataSource properties for $connectionName NOT updated, keeping the existing datasource in cache" }
                  }
                  connectionName
               }?.let {
                  logger.debug { "There is no datasource for $connectionName in the cache, no update is required." }
               }
            }

         }
      }
   }

   /**
    * Indicates if the config now present in the connection registry has changed for the current, stored state.
    * Does not create any actual connections, so safe to call repeatedly.
    */
   private fun configChanged(connectionName: String, previousState: HikariDataSource): Boolean {
      val connection = connectionRegistry.getConnection(connectionName)
      val jdbcUrlAndCredentials = connection.buildUrlAndCredentials()
      if (previousState.jdbcUrl != jdbcUrlAndCredentials.url || previousState.username != jdbcUrlAndCredentials.username || previousState.password != jdbcUrlAndCredentials.password) {
         return true
      }

      val connectionPoolProperties = connection.connectionPoolProperties()
      return connectionPoolProperties.connectionTimeout != previousState.connectionTimeout ||
         connectionPoolProperties.minimumIdleConnections != previousState.minimumIdle ||
         connectionPoolProperties.maxLifeTime != previousState.maxLifetime ||
         connectionPoolProperties.maxPoolSize != previousState.maximumPoolSize ||
         connectionPoolProperties.idleTimeout != previousState.idleTimeout
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

   /**
    * Returns a Hikari data source for the provided connectio name.
    * Be careful - this constructs an Hikari connection pool, which will throw an exception if there
    * are too many connected clients already
    */
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





