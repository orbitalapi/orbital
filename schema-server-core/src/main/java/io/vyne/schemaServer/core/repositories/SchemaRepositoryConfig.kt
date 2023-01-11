package io.vyne.schemaServer.core.repositories

import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig

data class SchemaRepositoryConfig(
   val file: FileSystemSchemaRepositoryConfig? = null,
   val git: GitSchemaRepositoryConfig? = null
) {
   fun repoCountDescription(): String {
      val fileRepos = file?.projects?.size ?: 0
      val gitRepos = git?.repositories?.size ?: 0
      return "$fileRepos file repositories and $gitRepos git repositories"
   }
}


interface SchemaRepositoryConfigLoader {
   fun load(): SchemaRepositoryConfig
   fun safeConfigJson(): String
   fun addFileSpec(fileSpec: FileSystemPackageSpec)

   fun addGitSpec(gitSpec: GitRepositoryConfig)
}

