package io.vyne.schemaServer.core.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.PackageIdentifier
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import io.vyne.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import io.vyne.utils.concat
import mu.KotlinLogging

class InMemorySchemaRepositoryConfigLoader(
   private var config: SchemaRepositoryConfig, private val eventDispatcher: RepositorySpecLifecycleEventDispatcher
) : SchemaRepositoryConfigLoader {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      emitInitialState()
   }

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

   override fun load(): SchemaRepositoryConfig = config
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

   override fun addGitSpec(gitSpec: GitRepositoryConfig) {
      config = config.copy(
         git = config.git!!.copy(
            repositories = config.git!!.repositories.concat(gitSpec)
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
