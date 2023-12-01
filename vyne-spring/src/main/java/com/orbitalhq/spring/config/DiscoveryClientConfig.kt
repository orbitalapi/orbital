package com.orbitalhq.spring.config

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.http.ServicesConfigProvider
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.http.SourceLoaderServicesConfigRepository
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaStore
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
   fun servicesConfigRepository(
      configPathProvider: ConfigPathProvider,
      schemaStore: SchemaStore,
      envVariablesConfig: EnvVariablesConfig
   ): SourceLoaderServicesConfigRepository {
      logger.info { "Registering a file based discovery client for Orbital services.  Using ${configPathProvider.servicesConfigFilePath.toFile().absolutePath}" }
      val systemConfigLoader = FileConfigSourceLoader(
         configPathProvider.servicesConfigFilePath,
         packageIdentifier = ConfigPathProvider.PACKAGE_IDENTIFIER,
         failIfNotFound = false
      )
      return SourceLoaderServicesConfigRepository(
         listOf(
            systemConfigLoader,
            FileConfigSourceLoader(envVariablesConfig.envVariablesPath, failIfNotFound = false, packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER),
            SchemaConfigSourceLoader(schemaStore, "env.conf"),
            SchemaConfigSourceLoader(schemaStore, "services.conf")
         ),
         systemConfigLoader
      )
   }

   @Bean
   fun fileBasedDiscoveryClient(configRepository: ServicesConfigProvider): FileBasedDiscoveryClient {
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
) {
   companion object {
      /**
       * Package id for the config we load at the system level (not for loading from config
       * sitting inside packages)
       */
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("com.orbitalhq.config/services/1.0.0")
   }
}

class TestDiscoveryClientConfig {
   @Bean
   @Primary
   fun tempFileConfigPathProvider(): ConfigPathProvider =
      ConfigPathProvider(Files.createTempFile("services", ".conf"))
}


