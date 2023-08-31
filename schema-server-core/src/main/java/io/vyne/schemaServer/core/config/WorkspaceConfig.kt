package io.vyne.schemaServer.core.config

import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "vyne.workspace")
data class WorkspaceConfig(
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
      WorkspaceConfig::class
   ]
)
class WorkspaceLoaderConfig {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // Place this here to allow overriding of the config loader in tests.
   @Bean
   fun configRepoLoader(
      workspaceConfig: WorkspaceConfig,
      eventDispatcher: RepositorySpecLifecycleEventDispatcher
   ): SchemaRepositoryConfigLoader {
      return if (workspaceConfig.projectFile != null) {
         logger.info { "A single-project workspace has been configured for ${workspaceConfig.projectFile}. Ignoring any other config from ${workspaceConfig.configFile}" }
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               FileSystemSchemaRepositoryConfig(
                  projects = listOf(FileSystemPackageSpec(workspaceConfig.projectFile!!, isEditable = true)),
               )
            ),
            eventDispatcher
         )
      } else {
         logger.info { "Using workspace config file at ${workspaceConfig.configFile}" }
         FileSchemaRepositoryConfigLoader(workspaceConfig.configFile, eventDispatcher = eventDispatcher)
      }
   }

}
