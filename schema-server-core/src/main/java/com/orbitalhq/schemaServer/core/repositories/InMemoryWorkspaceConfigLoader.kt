package com.orbitalhq.schemaServer.core.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.utils.concat
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.nio.file.Path

class InMemoryWorkspaceConfigLoader(
   private var config: WorkspaceConfig, private val eventDispatcher: ProjectSpecLifecycleEventDispatcher
) : WorkspaceConfigLoader {

   override val loaderStatus: Flux<LoaderStatus> = Flux.fromIterable(listOf(LoaderStatus.OK))

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

   override fun load(createDefaultIfAbsent: Boolean): WorkspaceConfig = config
   override fun safeConfigJson(): String {
      return jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config)
   }

   override fun addFileSpec(fileSpec: FileSystemPackageSpec): ModifyWorkspaceResponse {
      config = config.copy(
         file = config.file!!.copy(
            projects = config.file!!.projects.concat(fileSpec)
         )
      )
      eventDispatcher.fileRepositorySpecAdded(FileSpecAddedEvent(fileSpec, config.file!!))
      return ModifyWorkspaceResponse(ModifyProjectResponseStatus.Ok)
   }

   override fun addGitSpec(gitSpec: GitProjectStoreSpec): ModifyWorkspaceResponse {
      config = config.copy(
         git = config.gitConfigOrDefault.copy(
            repositories = config.gitConfigOrDefault.repositories.concat(gitSpec)
         )
      )
      eventDispatcher.gitRepositorySpecAdded(GitSpecAddedEvent(gitSpec, config.git!!))
      return ModifyWorkspaceResponse(ModifyProjectResponseStatus.Ok)
   }

   override fun removeGitRepository(
      repositoryName: String,
      packageIdentifier: PackageIdentifier
   ): List<PackageIdentifier> {

      config = config.copy(
         git = config.gitConfigOrDefault.copy(
            repositories = config.gitConfigOrDefault.repositories.filterNot { it.name == repositoryName }
         )
      )
      eventDispatcher.schemaSourceRemoved(listOf(packageIdentifier))
      return listOf(packageIdentifier)
   }

   override fun removeFileRepository(repositoryPath: Path, packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      config = config.copy(
         file = config.fileConfigOrDefault.copy(
            projects = config.fileConfigOrDefault.projects.filterNot {
               it.path == repositoryPath
            }
         )
      )
      eventDispatcher.schemaSourceRemoved(listOf(packageIdentifier))
      return listOf(packageIdentifier)

   }
   override fun removePushedRepository(identifier: PackageIdentifier): List<PackageIdentifier> {
      val identifiers = listOf(identifier)
      eventDispatcher.schemaSourceRemoved(identifiers)
      return identifiers
   }
}
