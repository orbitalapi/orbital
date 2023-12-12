package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig

/**
 * Class representing the workspace.conf file that defines
 * the various locations that taxi projects are loaded from
 */
data class WorkspaceConfig(
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

/**
 * Responsible for reading a workspace.conf file from somewhere.
 *
 * Workspace.conf is a HOCON file, which can be fetched from a local disk
 */
interface WorkspaceConfigLoader {
   fun load(): WorkspaceConfig
   fun safeConfigJson(): String
   fun addFileSpec(fileSpec: FileSystemPackageSpec)

   fun addGitSpec(gitSpec: GitProjectStoreSpec)
   fun removeGitRepository(repositoryName: String, packageIdentifier: PackageIdentifier): List<PackageIdentifier>
   fun removeFileRepository(packageIdentifier: PackageIdentifier): List<PackageIdentifier>

   /**
    * Indicates if this loader supports write operations (like adding git specs, etc)
    */
   val isReadOnly: Boolean

   val supportsWriteOperations: Boolean
      get() = !isReadOnly
}

