package io.orbital.station

import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.nio.file.Path

@Configuration
@Profile("orbital")
@ComponentScan(basePackageClasses = [OrbitalStationConfig::class, VersionedSourceLoader::class])
class OrbitalStationConfig {

   private val logger = KotlinLogging.logger {}

   @Bean
   fun configRepoLoader(
      @Value("\${vyne.repositories.config-file:repositories.conf}") configFilePath: Path,
      @Value("\${vyne.repositories.repository-path:#{null}}") repositoryHome: Path? = null,
      eventDispatcher: RepositorySpecLifecycleEventDispatcher
   ): SchemaRepositoryConfigLoader {
      return if (repositoryHome != null) {
         logger.info { "vyne.repositories.repository-path was set to $repositoryHome running a file-based repository from this path, ignoring any other config from $configFilePath" }
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               FileSystemSchemaRepositoryConfig(
                  projects = listOf(FileSystemPackageSpec(repositoryHome))
               )
            ),
            eventDispatcher
         )
      } else {
         logger.info { "Using repository config file at $configFilePath" }
         FileSchemaRepositoryConfigLoader(configFilePath, eventDispatcher = eventDispatcher)
      }
   }


}
