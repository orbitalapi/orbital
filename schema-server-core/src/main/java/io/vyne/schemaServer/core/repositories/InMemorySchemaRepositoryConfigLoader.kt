package io.vyne.schemaServer.core.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import io.vyne.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import io.vyne.utils.concat

class InMemorySchemaRepositoryConfigLoader(
   private var config: SchemaRepositoryConfig,
   private val eventDispatcher: RepositorySpecLifecycleEventDispatcher
) : SchemaRepositoryConfigLoader {
   override fun load(): SchemaRepositoryConfig = config
   override fun safeConfigJson(): String {
      return jacksonObjectMapper().writerWithDefaultPrettyPrinter()
         .writeValueAsString(config)
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
}
