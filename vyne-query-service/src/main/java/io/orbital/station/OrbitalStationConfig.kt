package io.orbital.station

import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schemaServer.core.VersionedSourceLoader
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.WorkspaceFileProjectConfig
import com.orbitalhq.schemaServer.core.repositories.FileWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.InMemoryWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
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
      eventDispatcher: ProjectSpecLifecycleEventDispatcher,
      projectManager: ProjectLoaderManager
   ): WorkspaceConfigLoader {
      return if (repositoryHome != null) {
         logger.info { "vyne.repositories.repository-path was set to $repositoryHome running a file-based repository from this path, ignoring any other config from $configFilePath" }
         return InMemoryWorkspaceConfigLoader(
            WorkspaceConfig(
               WorkspaceFileProjectConfig(
                  projects = listOf(FileProjectSpec(repositoryHome))
               )
            ),
            eventDispatcher
         )
      } else {
         logger.info { "Using repository config file at $configFilePath" }
         FileWorkspaceConfigLoader(configFilePath, eventDispatcher = eventDispatcher, projectManager = projectManager)
      }
   }


}
