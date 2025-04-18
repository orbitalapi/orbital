package com.orbitalhq.schemaServer.core.config

import com.orbitalhq.schema.publisher.ProjectLoaderManager
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.git.SimpleGitRepositoryConnectionConfig
import com.orbitalhq.schemaServer.core.repositories.FileWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.GitWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.exists

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
   val projectFile: Path? = null,

   /**
    * Allows defining a git repository for fetching Workspace settings.
    *
    * This is useful for production, read-only deployments.
    * Often (eg: ECS) attaching storage to a server and injecting a projectFile is cumbersome.
    * Additionally, fetching directly from Git aligns more with IAC / Immutable infrastructure.
    */
   val git: WorkspaceGitSettings? = null,

   val fileChangeDetectionMethod: FileChangeDetectionMethod = FileChangeDetectionMethod.WATCH,
   val filePollFrequency: Duration = Duration.ofSeconds(30),
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   fun createLoader(eventDispatcher: ProjectSpecLifecycleEventDispatcher,
                    projectManager: ProjectLoaderManager,
                    workspaceConfig: WorkspaceSettings): WorkspaceConfigLoader {
      return when {
         // MP: 16-Sep-24
         // When a user provided a project file, we used to skip creating a proper workspace (using an in-memory
         // one instead), which leads to all sorts of confusing bugs when they later try to do things like
         // add other projects etc.
         // Instead, we always create an on-disk workspace now.
         // If the user provides a project file, we add that to the workspace.
         // (see below)
//         workspaceConfig.projectFile != null -> {
//            logger.info { "A single-project workspace has been configured for ${workspaceConfig.projectFile}. Ignoring any other config from ${workspaceConfig.configFile}" }
//            return InMemoryWorkspaceConfigLoader(
//               WorkspaceConfig(
//                  WorkspaceFileProjectConfig(
//                     projects = listOf(FileProjectSpec(workspaceConfig.projectFile, isEditable = true)),
//                  )
//               ),
//               eventDispatcher
//            )
//         }

         git != null -> {
            logger.info { "Using a git-backed workspace config has been configured for $git" }
            GitWorkspaceConfigLoader(git, eventDispatcher = eventDispatcher, projectManager = projectManager)
         }

         else -> {
            val absolutePath = configFile.toAbsolutePath()
            logger.info { "Using workspace config file at ${configFile}, absolute path => $absolutePath" }
            val configLoader = FileWorkspaceConfigLoader(configFile, eventDispatcher = eventDispatcher, projectManager = projectManager)
            // Force the creation of the workspace file if missing
            if (!absolutePath.exists()) {
               logger.info { "Created blank workspace.conf file at $absolutePath" }
               configLoader.save(WorkspaceConfig.defaultEmpty(workspaceConfig))
            }
            try {
               val config = configLoader.load()
               if (projectFile != null) {
                  if (config.fileConfigOrDefault.projects.none { it.pathString.endsWith(projectFile.toString()) }) {
                     logger.info { "Workspace file at $absolutePath does not contain project $projectFile so adding it" }
                     configLoader.addFileSpec(
                        FileProjectSpec(
                           projectFile,
                           isEditable = true
                        )
                     )
                  }
               }
            } catch (e:Exception) {
               logger.warn(e) { "Error loading default workspace.conf file - project $projectFile has not been loaded" }
            }


            configLoader
         }
      }
   }
}

data class WorkspaceGitSettings(
   val url: URL,
   val branch: String,
   /**
    * The path to the workspace.conf file within the git repository
    */
   val path: Path = Paths.get("workspace.conf"),
   val checkoutPath:Path =  Paths.get("orbital/gitWorkspace/"),
   val pollDuration: Duration = Duration.ofSeconds(30)
) {
   val gitConfig =  SimpleGitRepositoryConnectionConfig(
      "workspace-config",
      url.toURI().toASCIIString(),
      branch
   )
}

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
      eventDispatcher: ProjectSpecLifecycleEventDispatcher,
      projectManager: ProjectLoaderManager
   ): WorkspaceConfigLoader {
      return workspaceConfig.createLoader(eventDispatcher, projectManager, workspaceConfig)
   }

}
