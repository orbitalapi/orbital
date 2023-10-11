package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitRepositorySpec
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig

data class SchemaRepositoryConfig(
   val file: FileSystemSchemaRepositoryConfig? = null,
   val git: GitSchemaRepositoryConfig? = null
) {
   fun repoCountDescription(): String {
      val fileRepos = file?.projects?.size ?: 0
      val gitRepos = git?.repositories?.size ?: 0
      return "$fileRepos file repositories and $gitRepos git repositories"
   }

   val gitConfigOrDefault:GitSchemaRepositoryConfig = git ?: GitSchemaRepositoryConfig.default()
   val fileConfigOrDefault:FileSystemSchemaRepositoryConfig = file ?: FileSystemSchemaRepositoryConfig()
}


interface SchemaRepositoryConfigLoader {
   fun load(): SchemaRepositoryConfig
   fun safeConfigJson(): String
   fun addFileSpec(fileSpec: FileSystemPackageSpec)

   fun addGitSpec(gitSpec: GitRepositorySpec)
   fun removeGitRepository(repositoryName: String, packageIdentifier: PackageIdentifier): List<PackageIdentifier>
   fun removeFileRepository(packageIdentifier: PackageIdentifier): List<PackageIdentifier>
}

