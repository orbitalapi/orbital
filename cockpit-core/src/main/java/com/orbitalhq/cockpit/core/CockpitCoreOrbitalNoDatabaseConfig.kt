package com.orbitalhq.cockpit.core

import com.orbitalhq.spring.config.RequiresOrbitalDbDisabled
import com.zaxxer.hikari.HikariConfig
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.hazelcast.HazelcastJpaDependencyAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@RequiresOrbitalDbDisabled
@EnableAutoConfiguration(exclude = [
    DataSourceAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    HazelcastJpaDependencyAutoConfiguration::class,
    JpaRepositoriesAutoConfiguration::class])
@Configuration
class CockpitCoreOrbitalNoDatabaseConfig {
    /**
     * In Dbless mode, we don't have any JPA beans inject automatically.
     * However, we still need a HikariConfig for our Database Connectors.
     */
    @Bean
    fun hikariConfigForJdbcConnectionConfig(): HikariConfig {
        val hikariConfig = HikariConfig()
        hikariConfig.apply {
            // set defaults users can customise them in connections.conf
            maximumPoolSize = 10
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            minimumIdle = 10
        }

        return hikariConfig
    }
}