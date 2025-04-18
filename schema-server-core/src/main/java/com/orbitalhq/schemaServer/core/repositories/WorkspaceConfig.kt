package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.config.WorkspaceSettings
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.WorkspaceFileProjectConfig
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.WorkspaceGitProjectConfig
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.schemaServer.repositories.FileProjectTestResponse
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path


private val logger = KotlinLogging.logger {}
/**
 * Class representing the workspace.conf file that defines
 * the various locations that taxi projects are loaded from
 */
data class WorkspaceConfig(
   val file: WorkspaceFileProjectConfig? = null,
   val git: WorkspaceGitProjectConfig? = null
) {

   companion object {
      fun defaultEmpty(workspaceConfig: WorkspaceSettings):WorkspaceConfig {
         return WorkspaceConfig(WorkspaceFileProjectConfig(
            changeDetectionMethod = workspaceConfig.fileChangeDetectionMethod,
            pollFrequency = workspaceConfig.filePollFrequency
         ), WorkspaceGitProjectConfig.default(workspaceConfig))
      }
   }
   fun repoCountDescription(): String {
      val fileRepos = file?.projects?.size ?: 0
      val gitRepos = git?.repositories?.size ?: 0
      return "$fileRepos file repositories and $gitRepos git repositories"
   }

   val gitConfigOrDefault:WorkspaceGitProjectConfig = git ?: WorkspaceGitProjectConfig.default()
   val fileConfigOrDefault:WorkspaceFileProjectConfig = file ?: WorkspaceFileProjectConfig()
}

enum class ModifyProjectResponseStatus {
   Ok,
   Warning,
   Failed
}
data class ModifyWorkspaceResponse(
   val status: ModifyProjectResponseStatus,
   val message: String? = null
)
/**
 * Responsible for reading a workspace.conf file from somewhere.
 *
 * Workspace.conf is a HOCON file, which can be fetched from a local disk
 */
interface WorkspaceConfigLoader {
   fun load(createDefaultIfAbsent: Boolean = true): WorkspaceConfig

   fun safeConfigJson(): String
   fun addFileSpec(fileSpec: FileProjectSpec): ModifyWorkspaceResponse

   fun addGitSpec(gitSpec: GitProjectSpec): ModifyWorkspaceResponse
   fun removeGitRepository(repositoryName: String, packageIdentifier: PackageIdentifier): List<PackageIdentifier>
   fun removeFileRepository(repositoryPath: Path, packageIdentifier: PackageIdentifier): List<PackageIdentifier>
   fun removePushedRepository(identifier: PackageIdentifier): List<PackageIdentifier>
   fun validateProjectExists(request: FileProjectStoreTestRequest): Mono<FileProjectTestResponse>

   val loaderStatus: Flux<LoaderStatus>

   /**
    * Indicates if this loader supports write operations (like adding git specs, etc)
    */
   val isReadOnly: Boolean

   val supportsWriteOperations: Boolean
      get() = !isReadOnly
}

