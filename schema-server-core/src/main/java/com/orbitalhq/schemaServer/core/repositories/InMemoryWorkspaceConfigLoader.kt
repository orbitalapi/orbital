package com.orbitalhq.schemaServer.core.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.utils.concat
import mu.KotlinLogging

class InMemoryWorkspaceConfigLoader(
   private var config: WorkspaceConfig, private val eventDispatcher: ProjectSpecLifecycleEventDispatcher
) : WorkspaceConfigLoader {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      emitInitialState()
   }

   override val isReadOnly: Boolean = false
   private fun emitInitialState() {
      logger.info { "In memory schema config running - registering initial state" }
      config.file?.let { fileConfig ->
         fileConfig.projects
            .forEach {
               logger.info { "Registering new file repo at ${it.path}" }
               eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(it, fileConfig))
            }
      }
      config.git?.let { gitConfig ->
         gitConfig.repositories.forEach {
            logger.info { "Registering new git repo at ${it.path}" }
            eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(it, gitConfig))
         }
      }
   }

   override fun load(): WorkspaceConfig = config
   override fun safeConfigJson(): String {
      return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config)
   }

   override fun addFileSpec(fileSpec: FileSystemPackageSpec) {
      config = config.copy(
         file = config.file!!.copy(
            projects = config.file!!.projects.concat(fileSpec)
         )
      )
      eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(fileSpec, config.file!!))
   }

   override fun addGitSpec(gitSpec: GitProjectStoreSpec) {
      config = config.copy(
         git = config.gitConfigOrDefault.copy(
            repositories = config.gitConfigOrDefault.repositories.concat(gitSpec)
         )
      )
      eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(gitSpec, config.git!!))
   }

   override fun removeGitRepository(
      repositoryName: String,
      packageIdentifier: PackageIdentifier
   ): List<PackageIdentifier> {
      TODO("Not yet implemented")
   }

   override fun removeFileRepository(packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      TODO("Not yet implemented")
   }
}
