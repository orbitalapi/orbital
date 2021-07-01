package io.vyne.queryService.connectors.jdbc

import io.vyne.connectors.jdbc.JdbcConnectionRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JdbcConnectionConfig {

   @Bean
   fun connectionRegistry(): JdbcConnectionRegistry {
      return JdbcConnectionRegistry()
   }
}
