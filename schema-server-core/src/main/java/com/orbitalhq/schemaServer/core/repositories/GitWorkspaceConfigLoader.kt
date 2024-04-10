package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.config.WorkspaceGitSettings
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.git.GitFileLoaderStatus
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitRef
import com.orbitalhq.schemaServer.core.git.GitRepoSync
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Path

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

   private val fileConfigLoader: FileWorkspaceConfigLoader

   private val gitStatusSink = Sinks.many().replay().latest<LoaderStatus>()
   override val loaderStatus: Flux<LoaderStatus>

   init {
      val workspaceConfigPath = gitSettings.checkoutPath.resolve(gitSettings.path)
      fileConfigLoader = FileWorkspaceConfigLoader(
         workspaceConfigPath,
         fallback,
         eventDispatcher,
         emitStateOnInit = false
      )
      repoSync.start(syncImmediately = syncUponInit)
         // We wait for the currentRef to change.
         // This will trigger on startup (ie., the first instance),
         // and then whenever the branch or current git ref updates.
         .distinctUntilChanged { status -> status.currentRef ?: GitRef.UNKNOWN }
         .subscribe { syncStatus ->
            if (!syncStatus.successful) {
               logger.warn { syncStatus.errorMessage!! }
               gitStatusSink.emitNext(LoaderStatus.error(syncStatus.errorMessage!!), Sinks.EmitFailureHandler.FAIL_FAST)
            } else {
               gitStatusSink.emitNext(LoaderStatus.OK, Sinks.EmitFailureHandler.FAIL_FAST)
               try {
                  logger.info { "Workspace from ${syncStatus.repository.redactedUrl} is now on ${syncStatus.currentRef} - refreshing workspace" }
                  fileConfigLoader.emitCurrentState()
               } catch (e: Exception) {
                  logger.info { "Failed to read workspace config: ${e.message}" }
               }

            }
         }

      val gitStatusMessages = gitStatusSink.asFlux()
      val fileStatusMessages = fileConfigLoader.loaderStatus

      loaderStatus = GitFileLoaderStatus.build(gitStatusMessages, fileStatusMessages)
      gitStatusSink.tryEmitNext(LoaderStatus.STARTING).orThrow()
   }

   override fun load(createDefaultIfAbsent: Boolean): WorkspaceConfig {
      val sync = repoSync.syncNow()
      if (sync.successful) {
         gitStatusSink.emitNext(LoaderStatus.OK, Sinks.EmitFailureHandler.FAIL_FAST)
         return loadWorkspace()
      } else {
         val message = "Git sync failed: ${sync.errorMessage}"
         gitStatusSink.emitNext(
            LoaderStatus.error(sync.errorMessage ?: "An unknown error occurred"),
            Sinks.EmitFailureHandler.FAIL_FAST
         )
         error(message)
      }
   }

   private fun loadWorkspace(): WorkspaceConfig {
      return fileConfigLoader.load(createDefaultIfAbsent = false)
   }


   override fun safeConfigJson(): String {
      return fileConfigLoader.safeConfigJson()
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

   override fun removeFileRepository(repositoryPath: Path, packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      error("Git workspaces are read only")
   }

   override fun removePushedRepository(identifier: PackageIdentifier): List<PackageIdentifier> {
      error("Git workspaces are read only")
   }
}
