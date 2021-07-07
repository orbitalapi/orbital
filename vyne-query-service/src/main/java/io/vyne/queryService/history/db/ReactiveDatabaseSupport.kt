package io.vyne.queryService.history.db

import com.rabbitmq.client.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator

/**
 * We use a local (disk-based) h2 database for storing query history.
 * This sets up the r2dbc config for persistence to the local store.
 */

/*
@Configuration
@EnableJdbcRepositories(basePackageClasses = [QueryHistoryRecordRepository::class])
class ReactiveDatabaseSupport {

   /**
    * Create the history schema if it doesn't exist.
    */
   @Bean
   fun initializer(@Qualifier("connectionFactory") connectionFactory: ConnectionFactory): ConnectionFactoryInitializer? {
      val initializer = ConnectionFactoryInitializer()
      initializer.setConnectionFactory(connectionFactory)
      val populator = CompositeDatabasePopulator()
      populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
      initializer.setDatabasePopulator(populator)
      return initializer
   }
}


 */