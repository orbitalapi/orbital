package io.vyne.spring.config

import io.vyne.http.ServicesConfigRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.*
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

/**
 * When Eureka is disabled, we spin up a file-based discovery client.
 *
 */
@Configuration
@ConditionalOnProperty("eureka.client.enabled", havingValue = "false", matchIfMissing = true)
@Import(ConfigPathProvider::class)
class DiscoveryClientConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @Bean
   fun servicesConfigRepository(configPathProvider: ConfigPathProvider): ServicesConfigRepository {
      logger.info { "Registering a file based discovery client for Vyne services.  Using ${configPathProvider.servicesConfigFilePath}" }
      val repository =
         ServicesConfigRepository(configPathProvider.servicesConfigFilePath, createConfigFileIfMissing = true)
      repository.watchForChanges()
      return repository
   }

   @Bean
   fun fileBasedDiscoveryClient(configRepository: ServicesConfigRepository): FileBasedDiscoveryClient {
      return FileBasedDiscoveryClient(configRepository)
   }

   @Bean
   fun fileBasedReactiveDiscoveryClient(discoveryClient: FileBasedDiscoveryClient): FileBasedReactiveDiscoveryClient {
      return FileBasedReactiveDiscoveryClient(discoveryClient)
   }


}

/**
 * Injecting a file path to a property in unit tests is really hard.
 * So, rather than use the @Value annotation directly in fileBasedDiscoveryClient() method,
 * we've extracted to a class, which allows for overriding in tests.
 * When testing, add @Import(TestDiscoveryClientConfig::class) to your test config class.
 */
@Component
class ConfigPathProvider(
   @Value("\${vyne.services.config-file:config/services.conf}") val servicesConfigFilePath: Path
)

class TestDiscoveryClientConfig {
   @Bean
   @Primary
   fun tempFileConfigPathProvider(): ConfigPathProvider =
      ConfigPathProvider(Files.createTempFile("services", ".conf"))
}


