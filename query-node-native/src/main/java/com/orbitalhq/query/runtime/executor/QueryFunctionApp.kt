package com.orbitalhq.query.runtime.executor

import com.zaxxer.hikari.HikariConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import com.orbitalhq.connectors.jdbc.registry.JdbcConnections
import com.orbitalhq.connectors.soap.SoapWsdlSourceConverter
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.models.facts.CascadingFactBag
import com.orbitalhq.query.QueryResponseMessage
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.spring.config.VyneSpringCacheConfiguration
import com.orbitalhq.spring.config.VyneSpringHazelcastConfiguration
import com.orbitalhq.spring.config.VyneSpringProjectionConfiguration
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import com.orbitalhq.utils.formatAsFileSize
import mu.KotlinLogging
import org.springframework.aot.hint.*
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.spring.config.ReactiveFeignAutoConfiguration

private val logger = KotlinLogging.logger {}

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
@RegisterReflectionForBinding(QueryResponseMessage::class)
class QueryFunctionApp {

   @Bean
   fun meterRegistry() = SimpleMeterRegistry()

   @Bean
   fun hikariConfig(): HikariConfig {
      return HikariConfig()
   }

   @Bean
   fun webClientBuilder(discoveryClient: DiscoveryClient): WebClient.Builder {
      return WebClient.builder()
   }

   @Bean
   fun formatSpecRegistry():FormatSpecRegistry = FormatSpecRegistry.default()

   @Bean
   fun sourceConverterRegistry(): SourceConverterRegistry = SourceConverterRegistry(
      setOf(
         TaxiSourceConverter,
         SoapWsdlSourceConverter
      ),
      registerWithStaticRegistry = true
   )
}

fun main(args: Array<String>) {
   logger.info { "Available processors (cores): ${Runtime.getRuntime().availableProcessors()}" }
   logger.info { "Total available memory: ${Runtime.getRuntime().freeMemory().formatAsFileSize}" }
   logger.info { "Max memory: ${Runtime.getRuntime().maxMemory().formatAsFileSize}" }
   logger.info { "Provided args: ${args.joinToString()}" }

   val envArgs = System.getenv().map { (key, value) -> "$key: $value" }
   logger.info { "Provided env variables: ${envArgs.joinToString()}" }
   runApplication<QueryFunctionApp>(*args)
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
         .registerType(TypeReference.of(QueryResponseMessage::class.java))
         .registerType(TypeReference.of(JdbcConnections::class.java))

      hints.reflection().registerType(CascadingFactBag::class.java, MemberCategory.INVOKE_DECLARED_METHODS)

      //
      hints.reflection()
         .registerField(QueryMessage::class.java.getField("Companion"))
         // as per: https://github.com/Kotlin/kotlinx.serialization/issues/1125#issuecomment-1364862908
         // Otherwise, at runtime, we get "kotlinx.serialization.SerializationException: Serializer for class 'QueryMessage' is not found."
         .registerMethod(QueryMessage.Companion::class.java.getMethod("serializer"), ExecutableMode.INVOKE)
         .registerType(QueryResponseMessage::class.java)
   }

}