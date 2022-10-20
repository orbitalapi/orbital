package io.orbital.station

import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
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
   ): io.vyne.schemaServer.core.SchemaRepositoryConfigLoader {
      return if (repositoryHome != null) {
         logger.info { "vyne.repositories.repository-path was set to $repositoryHome running a file-based repository from this path, ignoring any other config from $configFilePath" }
         return io.vyne.schemaServer.core.InMemorySchemaRepositoryConfigLoader(
            io.vyne.schemaServer.core.SchemaRepositoryConfig(
               FileSystemSchemaRepositoryConfig(
                  paths = listOf(repositoryHome)
               )
            )
         )
      } else {
         logger.info { "Using repository config file at $configFilePath" }
         io.vyne.schemaServer.core.FileSchemaRepositoryConfigLoader(configFilePath)
      }
   }

}
