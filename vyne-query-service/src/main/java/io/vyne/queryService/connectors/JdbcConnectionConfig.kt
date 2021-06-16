package io.vyne.queryService.connectors

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
