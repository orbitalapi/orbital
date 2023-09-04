package io.orbital.station

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.PostgreSQLContainer

/**
 * If the user hasn't specified a database setting, then we bootstrap a Postgres instance for them.
 * https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1
 * https://www.atomicjar.com/2023/05/spring-boot-3-1-0-testcontainers-for-testing-and-local-development/
 *
 */
@ConditionalOnProperty("vyne.database.username", matchIfMissing = true)
@Configuration
class LocalDbConfig {

   @Bean
   @ServiceConnection
   fun jdbcContainer(): PostgreSQLContainer<*> {
      return PostgreSQLContainer<Nothing>("postgres:12.3")
   }

}
