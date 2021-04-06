package io.vyne.queryService.history.db

import io.r2dbc.spi.ConnectionFactory
import io.vyne.queryService.history.db.entity.QueryHistoryRecordRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.connectionfactory.init.CompositeDatabasePopulator
import org.springframework.data.r2dbc.connectionfactory.init.ConnectionFactoryInitializer
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

/**
 * We use a local (disk-based) h2 database for storing query history.
 * This sets up the r2dbc config for persistence to the local store.
 */
@Configuration
@EnableR2dbcRepositories(basePackageClasses = [QueryHistoryRecordRepository::class])
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
