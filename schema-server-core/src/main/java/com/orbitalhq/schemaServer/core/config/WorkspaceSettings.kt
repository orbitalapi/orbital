package com.orbitalhq.schemaServer.core.config

import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.repositories.FileWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.InMemoryWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

/**
 * This is the command line / env-var settings passed to determine
 * where to read workspace config from.
 *
 * Not the actual workspace.conf file, which is modelled by WorkspaceConfig
 */
@ConfigurationProperties(prefix = "vyne.workspace")
data class WorkspaceSettings(
   /**
    * A path to the config file containing the workspace config.
    */
   val configFile: Path = Paths.get("workspace.conf"),

   /**
    * Path to a taxi.conf file.
    *
    * Bootstraps a single-file project, creating a standalone
    * workspace.
    *
    * If this is provided, the workspaceConfigPath is ignored.
    *
    * Useful for quickly bootstrapping demo projects, not intended
    * for production.
    */
   val projectFile: Path? = null
)


@Configuration
@EnableConfigurationProperties(
   value = [
      WorkspaceSettings::class
   ]
)
class WorkspaceLoaderConfig {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // Place this here to allow overriding of the config loader in tests.
   @Bean
   fun configRepoLoader(
      workspaceConfig: WorkspaceSettings,
      eventDispatcher: ProjectSpecLifecycleEventDispatcher
   ): WorkspaceConfigLoader {
      return if (workspaceConfig.projectFile != null) {
         logger.info { "A single-project workspace has been configured for ${workspaceConfig.projectFile}. Ignoring any other config from ${workspaceConfig.configFile}" }
         return InMemoryWorkspaceConfigLoader(
            WorkspaceConfig(
               FileSystemSchemaRepositoryConfig(
                  projects = listOf(FileSystemPackageSpec(workspaceConfig.projectFile, isEditable = true)),
               )
            ),
            eventDispatcher
         )
      } else {
         logger.info { "Using workspace config file at ${workspaceConfig.configFile}" }
         FileWorkspaceConfigLoader(workspaceConfig.configFile, eventDispatcher = eventDispatcher)
      }
   }

}
