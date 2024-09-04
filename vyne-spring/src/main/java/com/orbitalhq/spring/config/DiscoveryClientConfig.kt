package com.orbitalhq.spring.config

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VyneTypes
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.schema.consumer.ProjectManagerConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.publisher.ProjectLoaderManager
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.*
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Configuration
@Import(ConfigPathProvider::class)
class DiscoveryClientConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
      const val ORBITAL_INTERNAL_DISCOVERY_CLIENT = "ORBITAL_INTERNAL_DISCOVERY_CLIENT"
   }

   /**
    * Wires up a SourceLoaderServicesRegistry (which provides a ServicesConfig
    * used to power discover clients) that reads services.conf from a combination of:
    *  - A server-configured path (typically config/services.conf)
    *  - Env variables loaded from the server (typically config/env.conf)
    *  - Project based sources (both services.conf and env.conf)
    */
   @Bean
   fun servicesConfigRepository(
      configPathProvider: ConfigPathProvider,
      schemaStore: SchemaStore,
      envVariablesConfig: EnvVariablesConfig,
      additionalLoaders: List<ConfigSourceLoader>?,
      projectManager: ProjectLoaderManager
   ): SourceLoaderServicesRegistry {
      val projectManagerConfigSourceLoader = ProjectManagerConfigSourceLoader(
         schemaEventSource = schemaStore,
         projectManager = projectManager,
         filePattern = "services.conf"
      )
      val projectManagerEnvSourceLoader = ProjectManagerConfigSourceLoader(
         schemaEventSource = schemaStore,
         projectManager = projectManager,
         filePattern = "env.conf"
      )
      val builtinLoaders = listOf(
         FileConfigSourceLoader(
            envVariablesConfig.envVariablesPath,
            failIfNotFound = false,
            packageIdentifier = EnvVariablesConfig.PACKAGE_IDENTIFIER
         ),
         FileConfigSourceLoader(
            configPathProvider.servicesConfigFilePath,
            packageIdentifier = ConfigPathProvider.PACKAGE_IDENTIFIER,
            failIfNotFound = false
         ),
         projectManagerConfigSourceLoader,
         projectManagerEnvSourceLoader
      )

      val totalLoaders = additionalLoaders?.plus(builtinLoaders) ?: builtinLoaders
      return SourceLoaderServicesRegistry(totalLoaders, writerProviders = listOf(projectManagerConfigSourceLoader))
   }

   /**
    * A discovery client that ONLY evaluates the services.conf defined on the orbital server.
    * (ie., excludes any project-based services.conf files).
    * Use this for internal orbital components that are trying to talk to other Orbital components
    * (eg., prometheus or nebula)
    */
   @Bean(ORBITAL_INTERNAL_DISCOVERY_CLIENT)
   fun serverConfigDiscoveryClient(configPathProvider: ConfigPathProvider): FileBasedDiscoveryClient {
      logger.info { "Registering a file based discovery client for Vyne services.  Using ${configPathProvider.servicesConfigFilePath.toFile().absolutePath}" }
      val repository =
         ServicesConfigRepository(configPathProvider.servicesConfigFilePath, createConfigFileIfMissing = true)
      repository.watchForChanges()
      return FileBasedDiscoveryClient(repository)
   }

   @Bean
   fun discoveryClient(servicesRegistry: SourceLoaderServicesRegistry): SourceLoaderDiscoveryClient {
      return SourceLoaderDiscoveryClient(servicesRegistry)
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
      val PACKAGE_IDENTIFIER = PackageIdentifier.fromId("${VyneTypes.NAMESPACE}.config/services/1.0.0")
   }
}

class TestDiscoveryClientConfig {
   @Bean
   @Primary
   fun tempFileConfigPathProvider(): ConfigPathProvider =
      ConfigPathProvider(Files.createTempFile("services", ".conf"))
}


