package io.vyne.query.runtime.http

import com.zaxxer.hikari.HikariConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnections
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import mu.KotlinLogging
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.spring.config.ReactiveFeignAutoConfiguration

@SpringBootApplication(
   exclude = [JdbcRepositoriesAutoConfiguration::class,
      JdbcTemplateAutoConfiguration::class,
      DataSourceAutoConfiguration::class,
      ReactiveFeignAutoConfiguration::class
   ]
)
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   QueryAnalyticsConfig::class,
   VyneSpringProjectionConfiguration::class,
   VyneSpringHazelcastConfiguration::class,
)
@ImportRuntimeHints(NativeQueryNodeRuntimeHints::class)
class QueryNodeApp {

   @Bean
   fun meterRegistry() = SimpleMeterRegistry()

   @Bean
   fun hikariConfig(): HikariConfig {
      return HikariConfig()
   }

   @Bean
   fun webClientBuilder() = WebClient.builder()
}

fun main(args: Array<String>) {
   runApplication<QueryNodeApp>(*args)
}

class NativeQueryNodeRuntimeHints : RuntimeHintsRegistrar {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "Registering native runtime hints" }
   }

   override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
      hints.serialization()
         .registerType(TypeReference.of(JdbcConnections::class.java))
   }

}
