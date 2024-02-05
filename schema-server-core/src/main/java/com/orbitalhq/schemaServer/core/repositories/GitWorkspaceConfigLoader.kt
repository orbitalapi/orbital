package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.config.WorkspaceGitSettings
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitRepoSync
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KotlinLogging
import java.nio.file.Files

class GitWorkspaceConfigLoader(
   private val gitSettings: WorkspaceGitSettings,
   private val fallback: Config = ConfigFactory.systemEnvironment(),
   private val eventDispatcher: ProjectSpecLifecycleEventDispatcher,
   // Really, just for tests.
   // Should the repoSync task immediately attempt to sync?
   // In tests, this should be set to false to avoid timing issues
   syncUponInit: Boolean = true
) : WorkspaceConfigLoader {

   private val repoSync = GitRepoSync(
      gitSettings.checkoutPath,
      gitSettings.gitConfig,
      gitSettings.pollDuration
   )

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override val isReadOnly: Boolean = true

   init {
      repoSync.start(syncImmediately = syncUponInit)
         // We wait for the currentRef to change.
         // This will trigger on startup (ie., the first instance),
         // and then whenever the branch or current git ref updates.
         .distinctUntilChanged { status -> status.currentRef }
         .subscribe {syncStatus ->
            try {
               logger.info { "Workspace from ${syncStatus.repository.redactedUrl} is now on ${syncStatus.currentRef} - refreshing workspace" }
               fileConfigLoader().emitCurrentState()
            } catch (e: Exception) {
               logger.info { "Failed to read workspace config: ${e.message}" }
            }

         }
   }

   override fun load(): WorkspaceConfig {
      val sync = repoSync.syncNow()
      if (sync.successful) {
         return loadWorkspace()
      } else {
         error("Git sync failed: ${sync.errorMessage}")
      }
   }

   private fun loadWorkspace(): WorkspaceConfig {
      return fileConfigLoader().load()
   }

   private fun fileConfigLoader(): FileWorkspaceConfigLoader {
      val workspaceConfigPath = gitSettings.checkoutPath.resolve(gitSettings.path)
      if (!Files.exists(workspaceConfigPath)) {
         error("No workspace file exists at $workspaceConfigPath - Is the path within the git repo configured correctly? (Currently reading from ${gitSettings.path}")
      }
      return FileWorkspaceConfigLoader(
         workspaceConfigPath,
         fallback,
         eventDispatcher,
         emitStateOnInit = false
      )
   }

   override fun safeConfigJson(): String {
      return fileConfigLoader().safeConfigJson()
   }

   override fun addFileSpec(fileSpec: FileSystemPackageSpec) {
      error("Git workspaces are read only")
   }

   override fun addGitSpec(gitSpec: GitProjectStoreSpec) {
      error("Git workspaces are read only")
   }

   override fun removeGitRepository(
      repositoryName: String,
      packageIdentifier: PackageIdentifier
   ): List<PackageIdentifier> {
      error("Git workspaces are read only")
   }

   override fun removeFileRepository(packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      error("Git workspaces are read only")
   }
}
